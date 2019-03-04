import java.net.URI;
import java.net.URISyntaxException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.nio.ByteBuffer;
// import java.nio.channels.SocketChannel;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

public class Dewebsockify extends WebSocketClient {

    private Socket csocket;
    private InputStream csocketIn;
    private OutputStream csocketOut;

    public class InputThread extends Thread {
        public void run() {
            byte[] b = new byte[65536];
            int n;
            try{
                //csocketIn = csocket.getInputStream();
                //csocketOut = csocket.getOutputStream();
                while ((n = csocketIn.read(b)) >= 0) {
                    send(Arrays.copyOfRange(b, 0, n));
                }
            } catch (IOException e) {
                System.out.println("Exception caught when trying to read from socket");
                System.out.println(e.getMessage());
            }
            System.out.println("exiting");
            close();
        }
    }

    public Dewebsockify( URI serverURI, Socket csocket ) {
        super( serverURI );
        this.csocket = csocket;
    }

    public Dewebsockify( URI serverURI, Draft draft, Socket csocket ) {
        super( serverURI, draft );
        this.csocket = csocket;
    }

    @Override
    public void onOpen( ServerHandshake handshakedata ) {
                System.out.println( "opened connection to ws server" );
                //(new InputThread()).start();
                try{
                    csocketIn = csocket.getInputStream();
                    csocketOut = csocket.getOutputStream();
                    (new InputThread()).start();
                } catch (IOException e) {
                    System.out.println("Exception caught when trying to open socket streams");
                    System.out.println(e.getMessage());
                    close();
                }
    }

    @Override
    public void onMessage( String message ) {
        try {
            csocketOut.write(message.getBytes());
        } catch (IOException e) {
            System.out.println("Exception caught when trying to write to socket");
            System.out.println(e.getMessage());
        }
        // System.out.println( "received: " + message );
    }

    @Override
    public void onMessage( ByteBuffer bytes ) {
        try {
            csocketOut.write(bytes.array());
        } catch (IOException e) {
            System.out.println("Exception caught when trying to write to socket");
            System.out.println(e.getMessage());
        }
        // System.out.println( "received: " + message );
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        System.out.println( "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
        try {
            csocket.shutdownInput();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to shutdown socket");
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
    }

    public static void main( String[] args ) throws URISyntaxException, IOException {
        if (args.length != 2) {
            System.err.println("Usage: java dewebsockify <ws://server> <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[1]);
        String wsServer = args[0];

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            Socket clientSocket = serverSocket.accept();
            Dewebsockify c = new Dewebsockify( new URI( wsServer ), new Draft_6455(), clientSocket);
            c.connect();
            System.out.println("finished main thread");
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

}
