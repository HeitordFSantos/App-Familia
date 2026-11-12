package com.appfamilia.feature.dependentes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appfamilia.R;
import com.appfamilia.core.rede.Modelos;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DependenteAdapter extends RecyclerView.Adapter<DependenteAdapter.Linha> {

    public interface Acao {
        void em(Modelos.Dependente dependente);
    }

    private final List<Modelos.Dependente> itens = new ArrayList<>();
    private final Acao aoTocar;
    private final Acao aoSegurar;

    public DependenteAdapter(Acao aoTocar, Acao aoSegurar) {
        this.aoTocar = aoTocar;
        this.aoSegurar = aoSegurar;
    }

    public void atualizar(List<Modelos.Dependente> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View view = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_dependente, pai, false);
        return new Linha(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        Modelos.Dependente dependente = itens.get(posicao);
        linha.nome.setText(dependente.nome);
        linha.detalhe.setText(dataBr(dependente.dataNascimento) + "  ·  " + idade(dependente.dataNascimento));
        linha.itemView.setOnClickListener(v -> aoTocar.em(dependente));
        linha.itemView.setOnLongClickListener(v -> {
            aoSegurar.em(dependente);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class Linha extends RecyclerView.ViewHolder {
        final TextView nome;
        final TextView detalhe;

        Linha(View view) {
            super(view);
            nome = view.findViewById(R.id.texto_nome);
            detalhe = view.findViewById(R.id.texto_detalhe);
        }
    }

    /** "2024-03-10" vira "10/03/2024". */
    static String dataBr(String iso) {
        String[] p = iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : iso;
    }

    /** Idade por extenso a partir de uma data AAAA-MM-DD. */
    static String idade(String iso) {
        String[] p = iso.split("-");
        if (p.length != 3) {
            return "";
        }
        Calendar hoje = Calendar.getInstance();
        int meses = (hoje.get(Calendar.YEAR) - Integer.parseInt(p[0])) * 12
                + (hoje.get(Calendar.MONTH) + 1 - Integer.parseInt(p[1]));
        if (hoje.get(Calendar.DAY_OF_MONTH) < Integer.parseInt(p[2])) {
            meses--;
        }
        if (meses < 1) {
            return "menos de 1 mês";
        }
        int anos = meses / 12;
        int resto = meses % 12;
        String textoAnos = anos == 1 ? "1 ano" : anos + " anos";
        String textoMeses = resto == 1 ? "1 mês" : resto + " meses";
        if (anos == 0) {
            return textoMeses;
        }
        return resto == 0 ? textoAnos : textoAnos + " e " + textoMeses;
    }
}
