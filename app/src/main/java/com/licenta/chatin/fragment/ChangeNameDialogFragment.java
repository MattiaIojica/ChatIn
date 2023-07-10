package com.licenta.chatin.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.DocumentChange;
import com.licenta.chatin.R;
import com.licenta.chatin.models.ChatMessage;
import com.licenta.chatin.utilities.Constants;

public class ChangeNameDialogFragment  extends DialogFragment {

    private EditText newNameEditText;
    private EditText currentPasswordEditText;

    private ChangeNameDialogFragment.OnChangeNameListener listener;

    public interface OnChangeNameListener {
        void onChangeName(String newName, String currentPassword);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ChangeNameDialogFragment.OnChangeNameListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnConfirmPasswordListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_name, null);

        newNameEditText = view.findViewById(R.id.editTextNewName);
        currentPasswordEditText = view.findViewById(R.id.editTextCurrentPassword);

        builder.setView(view)
                .setTitle("Change Password")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = newNameEditText.getText().toString().trim();
                        String currentPassword = currentPasswordEditText.getText().toString().trim();
                        listener.onChangeName(newName, currentPassword);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }
}