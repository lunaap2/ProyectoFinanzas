package com.example.finanzas;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;


public class TransaccionAdapter extends RecyclerView.Adapter<TransaccionAdapter.TransaccionViewHolder> {

    public interface OnItemListener {
        void onItemClick(Transaccion t);       // click normal -> editar

        void onItemLongClick(Transaccion t);    // click largo -> eliminar
    }

    private List<Transaccion> transacciones;
    private final OnItemListener listener;

    public TransaccionAdapter(List<Transaccion> transacciones, OnItemListener listener) {
        this.transacciones = transacciones;
        this.listener = listener;
    }


    @NonNull
    @Override
    public TransaccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaccion, parent, false);
        return new TransaccionViewHolder(vista);
    }


    @Override
    public void onBindViewHolder(@NonNull TransaccionViewHolder holder, int position) {
        Transaccion t = transacciones.get(position);

        holder.txtConcepto.setText(t.getConcepto());
        holder.txtTipo.setText(t.getTipo());

        // Color e indicador segun sea ingreso (+ verde) o gasto (- rojo).
        if (t.esIngreso()) {
            holder.txtMonto.setText(String.format(Locale.getDefault(), "+ $%.2f", t.getMonto()));
            holder.txtMonto.setTextColor(Color.parseColor("#2E7D32")); // verde
            holder.txtTipo.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.txtMonto.setText(String.format(Locale.getDefault(), "- $%.2f", t.getMonto()));
            holder.txtMonto.setTextColor(Color.parseColor("#C62828")); // rojo
            holder.txtTipo.setTextColor(Color.parseColor("#C62828"));
        }

        // Eventos: click para editar, boton (o click largo) para eliminar.
        holder.itemView.setOnClickListener(v -> listener.onItemClick(t));
        holder.btnEliminar.setOnClickListener(v -> listener.onItemLongClick(t));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(t);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transacciones.size();
    }

    //reemplaza los datos y refresca la lista (se usa al recargar desde la BD)
    public void actualizarLista(List<Transaccion> nuevas) {
        this.transacciones = nuevas;
        notifyDataSetChanged();
    }


    static class TransaccionViewHolder extends RecyclerView.ViewHolder {
        TextView txtConcepto;
        TextView txtTipo;
        TextView txtMonto;
        android.widget.ImageButton btnEliminar;

        TransaccionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtConcepto = itemView.findViewById(R.id.txtConcepto);
            txtTipo = itemView.findViewById(R.id.txtTipo);
            txtMonto = itemView.findViewById(R.id.txtMonto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
