package com.example.finanzasluna;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;


public class TransaccionAdapter extends RecyclerView.Adapter<TransaccionAdapter.TransaccionViewHolder> {

    public interface OnItemListener {
        void onItemClick(Transaccion t);       // click normal -> editar

        void onItemLongClick(Transaccion t);    // click largo / boton -> eliminar
    }

    private List<Transaccion> transacciones;
    private final OnItemListener listener;
    // RBAC: solo el rol Administrador puede ver/usar el boton de eliminar.
    private final boolean esAdmin;

    public TransaccionAdapter(List<Transaccion> transacciones, OnItemListener listener, boolean esAdmin) {
        this.transacciones = transacciones;
        this.listener = listener;
        this.esAdmin = esAdmin;
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

        // Color, icono y tarjeta segun sea ingreso (verde) o gasto (rojo).
        if (t.esIngreso()) {
            holder.txtMonto.setText(String.format(Locale.getDefault(), "+ $%.2f", t.getMonto()));
            holder.txtMonto.setTextColor(Color.parseColor("#2E7D32"));
            holder.txtTipo.setTextColor(Color.parseColor("#2E7D32"));
            holder.imgIcono.setImageResource(R.drawable.ic_arrow_upward);
            holder.imgIcono.setColorFilter(Color.parseColor("#2E7D32"));
            holder.cardIcono.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
        } else {
            holder.txtMonto.setText(String.format(Locale.getDefault(), "- $%.2f", t.getMonto()));
            holder.txtMonto.setTextColor(Color.parseColor("#C62828"));
            holder.txtTipo.setTextColor(Color.parseColor("#C62828"));
            holder.imgIcono.setImageResource(R.drawable.ic_arrow_downward);
            holder.imgIcono.setColorFilter(Color.parseColor("#C62828"));
            holder.cardIcono.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
        }

        // Miniatura con Glide: placeholder/error evitan que, al reciclar la vista, se vea
        // por un instante la foto de otra transaccion antes de que cargue la correcta.
        Glide.with(holder.imgFoto.getContext())
                .load(t.getFotoUrl())
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_placeholder)
                .centerCrop()
                .into(holder.imgFoto);

        // RBAC: el boton de eliminar solo existe para el rol Administrador.
        holder.btnEliminar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

        // Eventos: click para editar, boton (o click largo) para eliminar.
        holder.itemView.setOnClickListener(v -> listener.onItemClick(t));
        holder.btnEliminar.setOnClickListener(v -> listener.onItemLongClick(t));
        holder.itemView.setOnLongClickListener(v -> {
            if (esAdmin) {
                listener.onItemLongClick(t);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transacciones.size();
    }

    // reemplaza los datos y refresca la lista (se usa desde el SnapshotListener de Firestore)
    public void actualizarLista(List<Transaccion> nuevas) {
        this.transacciones = nuevas;
        notifyDataSetChanged();
    }


    static class TransaccionViewHolder extends RecyclerView.ViewHolder {
        TextView txtConcepto;
        TextView txtTipo;
        TextView txtMonto;
        ImageView imgIcono;
        ImageView imgFoto;
        MaterialCardView cardIcono;
        ImageButton btnEliminar;

        TransaccionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtConcepto = itemView.findViewById(R.id.txtConcepto);
            txtTipo = itemView.findViewById(R.id.txtTipo);
            txtMonto = itemView.findViewById(R.id.txtMonto);
            imgIcono = itemView.findViewById(R.id.imgIcono);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            cardIcono = itemView.findViewById(R.id.cardIcono);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
