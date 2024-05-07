package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;

public class MainActivity extends AppCompatActivity {

    protected static String server = "192.168.1.133";
    protected static int port = 7070;
    private KeyPair keyPair;

    private boolean validateInput(String amount) {
        try {
            int amountValue = Integer.parseInt(amount);
            return amountValue >= 0 && amountValue <= 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
           KeyPair keyPair = ClientKeyGenerator.generateKeys(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        View button = findViewById(R.id.button_send); // Capturamos el boton de Enviar

        button.setOnClickListener(new View.OnClickListener() { // Llama al listener del boton Enviar
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
    }

    private void showDialog() throws Resources.NotFoundException {

        EditText camas = (EditText) findViewById(R.id.numCamas);
        String numCamas = camas.getText().toString().trim().isEmpty() ? "0" : camas.getText().toString().trim();

        EditText mesas =  (EditText) findViewById(R.id.numMesas);
        String numMesas = mesas.getText().toString().trim().isEmpty() ? "0" : mesas.getText().toString().trim();

        EditText sillas =  (EditText) findViewById(R.id.numSillas);
        String numSillas = sillas.getText().toString().trim().isEmpty() ? "0" : sillas.getText().toString().trim();

        EditText sillones = (EditText) findViewById(R.id.numSillones);
        String numSillones = sillones.getText().toString().trim().isEmpty() ? "0" : sillones.getText().toString().trim();

        EditText cliente = (EditText) findViewById(R.id.editTextNumber2);
        String numCliente = cliente.getText().toString().trim();

        if (!validateInput(numCamas) || !validateInput(numMesas) || !validateInput(numSillas) || !validateInput(numSillones)) {
            Toast.makeText(getApplicationContext(), "Los valores deben ser números enteros entre 0 y 300", Toast.LENGTH_SHORT).show();
            return;

        } else if (numCliente.isEmpty()) {
            Toast.makeText(getApplicationContext(), "El identificador de cliente es obligatorio", Toast.LENGTH_SHORT).show();
            return;

        } else {
            new AlertDialog.Builder(this)
                .setTitle("Enviar")
                .setMessage("Se va a proceder al envio")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() { // Catch ok button and send information

                        public void onClick(DialogInterface dialog, int whichButton) {

                            // 1. Extraer los datos de la vista

                            String message = numCamas + "," + numMesas + "," + numSillas + "," + numSillones + "," + numCliente;

                            // 2. Firmar los datos

                            String firmaStr = "";

                            try {
                                Signature sg = Signature.getInstance("SHA256withRSA");
                                try {
                                    sg.initSign(keyPair.getPrivate());
                                    sg.update(message.getBytes());
                                    byte[] firma = sg.sign();
                                    firmaStr = Base64.encodeToString(firma, Base64.DEFAULT);

                                } catch (InvalidKeyException | SignatureException e) {
                                    Toast.makeText(MainActivity.this, "Firma no realizada incorrectamente / credenciales incorrectas", Toast.LENGTH_SHORT).show();
                                }
                            } catch (NoSuchAlgorithmException e) {
                                Toast.makeText(MainActivity.this, "Error durante la firma", Toast.LENGTH_SHORT).show();
                            }

                            // 3. Enviar los datos

                            try{
                                Task task = new Task(message + "," + firmaStr, server, port, new Task.OnRequestListener() {
                                    @Override
                                    public void onRequestResult(String result) {
                                        try {
                                            Toast.makeText(MainActivity.this, "Petición OK", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Toast.makeText(MainActivity.this, "Petición INCORRECTA", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onRequestFailure(String errorMessage) {
                                        // Manejar el error de solicitud aquí
                                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                task.execute();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            )
            .setNegativeButton(android.R.string.no, null)
            .show();
        }
    }
}