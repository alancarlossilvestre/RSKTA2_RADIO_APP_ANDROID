package com.example.radio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.radio.model.Program;
import com.example.radio.presenter.ProgramsPresenter;
import com.example.radio.view.ProgramsView;

import java.util.ArrayList;
import java.util.List;

public class ProgramsFragment extends Fragment implements ProgramsView {

    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private ProgramsPresenter presenter;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_programs, container, false);

        recyclerView = view.findViewById(R.id.horizontalRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new CardAdapter();
        recyclerView.setAdapter(adapter);

        presenter = new ProgramsPresenter(this);
        presenter.loadPrograms();

        return view;
    }

    @Override
    public void showPrograms(List<Program> programs) {
        adapter.updateData(programs);
    }

    @Override
    public void showProgramsError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Adapter interno
    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

        private List<Program> items = new ArrayList<>();

        class CardViewHolder extends RecyclerView.ViewHolder {
            TextView title_program, horus_program, description_program;
            CardViewHolder(View itemView) {
                super(itemView);
                title_program = itemView.findViewById(R.id.program_name);
                horus_program = itemView.findViewById(R.id.horus);
                description_program = itemView.findViewById(R.id.horus);
            }
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_programs_item, parent, false);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            Program program = items.get(position);
            holder.title_program.setText(program.getName_program());
            holder.horus_program.setText(program.getHorus_program());
            holder.description_program.setText(program.getDescription_program());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void updateData(List<Program> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }
    }

}
