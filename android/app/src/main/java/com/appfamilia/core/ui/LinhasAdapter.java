package com.appfamilia.core.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appfamilia.R;
import java.util.ArrayList;
import java.util.List;

/** Adapter único das listas dos módulos. */
public class LinhasAdapter extends RecyclerView.Adapter<LinhasAdapter.Suporte> {

    public interface Acao {
        void em(Linha linha);
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final Acao aoTocar;
    private final Acao aoSegurar;

    public LinhasAdapter(Acao aoTocar, Acao aoSegurar) {
        this.aoTocar = aoTocar;
        this.aoSegurar = aoSegurar;
    }

    public void atualizar(List<Linha> novas) {
        linhas.clear();
        linhas.addAll(novas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Suporte onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Suporte(LayoutInflater.from(pai.getContext()).inflate(R.layout.item_linha, pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Suporte suporte, int posicao) {
        Linha linha = linhas.get(posicao);
        suporte.titulo.setText(linha.titulo);
        boolean temDetalhe = linha.detalhe != null && !linha.detalhe.isEmpty();
        suporte.detalhe.setText(temDetalhe ? linha.detalhe : "");
        suporte.detalhe.setVisibility(temDetalhe ? View.VISIBLE : View.GONE);
        suporte.marcador.setVisibility(linha.marcado == null ? View.GONE : View.VISIBLE);
        suporte.marcador.setChecked(Boolean.TRUE.equals(linha.marcado));
        suporte.itemView.setOnClickListener(v -> aoTocar.em(linha));
        suporte.itemView.setOnLongClickListener(v -> {
            aoSegurar.em(linha);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return linhas.size();
    }

    static class Suporte extends RecyclerView.ViewHolder {
        final CheckBox marcador;
        final TextView titulo;
        final TextView detalhe;

        Suporte(View view) {
            super(view);
            marcador = view.findViewById(R.id.marcador);
            titulo = view.findViewById(R.id.texto_titulo);
            detalhe = view.findViewById(R.id.texto_detalhe);
        }
    }
}
