package com.example.radio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ProgramsFragment extends Fragment {


    private RecyclerView recyclerView;
    private CardAdapter adapter;

    public ProgramsFragment() {
        // Required empty public constructor
    }

    public static ProgramsFragment newInstance(String param1, String param2) {
        ProgramsFragment fragment = new ProgramsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_programs, container, false);

        recyclerView = view.findViewById(R.id.horizontalRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        adapter = new CardAdapter();
        recyclerView.setAdapter(adapter);

        // Simulación de datos (normalmente lo manda el Presenter)
        List<String> sampleData = new ArrayList<>();
        sampleData.add("Nombre del programa 1");
        sampleData.add("Card 2");
        sampleData.add("Card 3");
        sampleData.add("Card 4");

        adapter.updateData(sampleData);

        return view;
    }

    // Adapter interno
    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

        private List<String> items = new ArrayList<>();

        class CardViewHolder extends RecyclerView.ViewHolder {
            TextView title_program;
            TextView horus_program;

            CardViewHolder(View itemView) {
                super(itemView);
                title_program = itemView.findViewById(R.id.program_name);
                horus_program = itemView.findViewById(R.id.horus);
            }
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_programs_item, parent, false);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            holder.title_program.setText(items.get(position));
            // Aquí puedes asignar imágenes dinámicamente si quieres
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void updateData(List<String> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }
    }
}