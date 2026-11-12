package com.appfamilia.core.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

/**
 * Tira uma foto com a câmera ou escolhe uma da galeria, reduz o tamanho e entrega em base64,
 * pronta para enviar ao backend. Precisa ser criado no onCreate da tela.
 */
public class SeletorDeFoto {

    public interface Retorno {
        void foto(String jpegEmBase64);
    }

    /** Lado maior da imagem enviada: suficiente para ler texto sem pesar no envio. */
    private static final int LADO_MAXIMO = 1600;

    private final AppCompatActivity tela;
    private final Retorno retorno;
    private final ActivityResultLauncher<Uri> camera;
    private final ActivityResultLauncher<String> galeria;
    private Uri destinoDaCamera;

    public SeletorDeFoto(AppCompatActivity tela, Retorno retorno) {
        this.tela = tela;
        this.retorno = retorno;
        camera = tela.registerForActivityResult(new ActivityResultContracts.TakePicture(), tirou -> {
            if (Boolean.TRUE.equals(tirou) && destinoDaCamera != null) {
                preparar(destinoDaCamera);
            }
        });
        galeria = tela.registerForActivityResult(new ActivityResultContracts.GetContent(), escolhida -> {
            if (escolhida != null) {
                preparar(escolhida);
            }
        });
    }

    public void abrir() {
        new MaterialAlertDialogBuilder(tela)
                .setTitle("De onde vem a foto?")
                .setItems(new CharSequence[]{"Tirar foto agora", "Escolher da galeria"}, (dialogo, qual) -> {
                    if (qual == 0) {
                        tirar();
                    } else {
                        galeria.launch("image/*");
                    }
                })
                .show();
    }

    private void tirar() {
        try {
            File pasta = new File(tela.getCacheDir(), "fotos");
            pasta.mkdirs();
            destinoDaCamera = FileProvider.getUriForFile(tela, tela.getPackageName() + ".arquivos",
                    new File(pasta, "foto.jpg"));
            camera.launch(destinoDaCamera);
        } catch (Exception semCamera) {
            Toast.makeText(tela, "Não foi possível abrir a câmera. Escolha a foto pela galeria.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Reduzir a imagem é lento; roda fora da thread da tela. */
    private void preparar(Uri foto) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String base64;
            try {
                base64 = reduzir(foto);
            } catch (Exception ilegivel) {
                base64 = null;
            }
            String pronto = base64;
            tela.runOnUiThread(() -> {
                if (pronto == null) {
                    Toast.makeText(tela, "Não foi possível abrir esta foto", Toast.LENGTH_LONG).show();
                } else {
                    retorno.foto(pronto);
                }
            });
        });
    }

    private String reduzir(Uri foto) throws IOException {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        try (InputStream entrada = tela.getContentResolver().openInputStream(foto)) {
            BitmapFactory.decodeStream(entrada, null, medidas);
        }
        // Primeiro corta pela metade enquanto sobrar; depois ajusta para o lado máximo exato
        int amostra = 1;
        while (Math.max(medidas.outWidth, medidas.outHeight) / (amostra * 2) >= LADO_MAXIMO) {
            amostra *= 2;
        }
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostra;
        Bitmap imagem;
        try (InputStream entrada = tela.getContentResolver().openInputStream(foto)) {
            imagem = BitmapFactory.decodeStream(entrada, null, opcoes);
        }
        if (imagem == null) {
            throw new IOException("imagem ilegível");
        }
        float escala = Math.min(1f, LADO_MAXIMO / (float) Math.max(imagem.getWidth(), imagem.getHeight()));
        Matrix ajuste = new Matrix();
        ajuste.postScale(escala, escala);
        ajuste.postRotate(rotacao(foto));
        imagem = Bitmap.createBitmap(imagem, 0, 0, imagem.getWidth(), imagem.getHeight(), ajuste, true);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        imagem.compress(Bitmap.CompressFormat.JPEG, 80, saida);
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP);
    }

    /** Fotos de câmera guardam a rotação nos metadados; sem aplicar, a imagem iria deitada. */
    private int rotacao(Uri foto) {
        try (InputStream entrada = tela.getContentResolver().openInputStream(foto)) {
            if (entrada == null) {
                return 0;
            }
            int orientacao = new ExifInterface(entrada)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacao) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception semMetadados) {
            return 0;
        }
    }
}
