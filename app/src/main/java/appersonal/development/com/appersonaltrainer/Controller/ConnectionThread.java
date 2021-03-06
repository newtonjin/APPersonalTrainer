package appersonal.development.com.appersonaltrainer.Controller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import appersonal.development.com.appersonaltrainer.Activities.MeusTreinosActivity;
import appersonal.development.com.appersonaltrainer.Activities.ReceberTreinoActivity;

public class ConnectionThread extends Thread {

    private Handler h = new Handler();

    private byte[] treinos;
    //    private int bytes;
    private BluetoothSocket btSocket = null;
    private BluetoothServerSocket btServerSocket = null;
    private OutputStream output = null;
    private String btDevAddress = null;
    private boolean server;
    public boolean running = false;

    /*  Este construtor prepara o dispositivo para atuar como servidor.
     */
    public ConnectionThread() {
        this.server = true;
    }

    /*  Este construtor prepara o dispositivo para atuar como cliente.
        Tem como argumento uma string contendo o endereço MAC do dispositivo
    Bluetooth para o qual deve ser solicitada uma conexão.
     */
    public ConnectionThread(String btDevAddress) {

        this.server = false;
        this.btDevAddress = btDevAddress;
    }

    /*  O método run() contem as instruções que serão efetivamente realizadas
    em uma nova thread.
     */
    public void run() {

        /*  Anuncia que a thread está sendo executada.
            Pega uma referência para o adaptador Bluetooth padrão.
         */
        this.running = true;
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        /*  Determina que ações executar dependendo se a thread está configurada
        para atuar como servidor ou cliente.
         */
        String myUUID = "00001101-0000-1000-8000-00805F9B34FB";
        if (this.server) {

            /*  Servidor.
             */
            try {

                /*  Cria um socket de servidor Bluetooth.
                    O socket servidor será usado apenas para iniciar a conexão.
                    Permanece em estado de espera até que algum cliente
                estabeleça uma conexão.
                 */
                btServerSocket = btAdapter.listenUsingRfcommWithServiceRecord("Super Bluetooth", UUID.fromString(myUUID));
                btSocket = btServerSocket.accept();

                /*  Se a conexão foi estabelecida corretamente, o socket
                servidor pode ser liberado.
                 */
                if (btSocket != null) {
                    btServerSocket.close();
                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }


        } else {

            /*  Cliente.
             */
            try {

                /*  Obtem uma representação do dispositivo Bluetooth com
                endereço btDevAddress.
                    Cria um socket Bluetooth.
                 */
                BluetoothDevice btDevice = btAdapter.getRemoteDevice(btDevAddress);
                btSocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString(myUUID));

                /*  Envia ao sistema um comando para cancelar qualquer processo
                de descoberta em execução.
                 */
                btAdapter.cancelDiscovery();

                /*  Solicita uma conexão ao dispositivo cujo endereço é
                btDevAddress.
                    Permanece em estado de espera até que a conexão seja
                estabelecida.
                 */
                if (btSocket != null)
                    btSocket.connect();

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }

        }

        /*  Pronto, estamos conectados! Agora, só precisamos gerenciar a conexão.
            ...
        */

        if (btSocket != null) {

            /*  Envia um código para a Activity principal informando que a
            a conexão ocorreu com sucesso.
             */
            toMainActivity("---S".getBytes());

            try {

                /*  Obtem referências para os fluxos de entrada e saída do
                socket Bluetooth.
                 */
                InputStream input = btSocket.getInputStream();
                output = btSocket.getOutputStream();

                /*  Cria um byte array para armazenar temporariamente uma
                mensagem recebida.
                    O inteiro bytes representará o número de bytes lidos na
                última mensagem recebida.
                 */
                byte[] buffer = new byte[1024];
                int bytes;

                /*  Permanece em estado de espera até que uma mensagem seja
                recebida.
                    Armazena a mensagem recebida no buffer.
                    Envia a mensagem recebida para a Activity principal, do
                primeiro ao último byte lido.
                    Esta thread permanecerá em estado de escuta até que
                a variável running assuma o valor false.
                 */
                while (running) {
                    bytes = input.read(buffer);
                    toReceberActivity(Arrays.copyOfRange(buffer, 0, bytes));
                }

            } catch (IOException e) {

                /*  Caso ocorra alguma exceção, exibe o stack trace para debug.
                    Envia um código para a Activity principal, informando que
                a conexão falhou.
                 */
                e.printStackTrace();
                toMainActivity("---N".getBytes());
            }
        }

    }

    /*  Utiliza um handler para enviar um byte array à Activity principal.
        O byte array é encapsulado em um Bundle e posteriormente em uma Message
    antes de ser enviado.
     */
    private void toMainActivity(byte[] data) {

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putByteArray("data", data);
        message.setData(bundle);
        MeusTreinosActivity.handler.sendMessage(message);
    }

    //    h.postDelayed(receberMensagem, 10);
    private void toReceberActivity(byte[] treino) {

        treinos = treino;
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putByteArray("treino", treinos);
                message.setData(bundle);
                ReceberTreinoActivity.handler.sendMessage(message);
            }
        }, 5);

    }

    /*  Método utilizado pela Activity principal para transmitir uma mensagem ao
     outro lado da conexão.
        A mensagem deve ser representada por um byte array.
     */
    public void write(byte[] data) {

        if (output != null) {
            try {
                /*  Transmite a mensagem.
                 */
                output.write(data);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("falhou exception 2");
            }
        } else {

            /*  Envia à Activity principal um código de erro durante a conexão.
             */
            toMainActivity("---N".getBytes());
            System.out.println("falhou");
        }
    }

    /*  Método utilizado pela Activity principal para encerrar a conexão
     */
    public void cancel() {

        try {
            running = false;
            if (btServerSocket != null)
                btServerSocket.close();
            if (btSocket != null)
                btSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }
}
