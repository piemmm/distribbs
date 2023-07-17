package org.ka2ddo.ax25.test;

import org.ka2ddo.ax25.*;
import org.ka2ddo.ax25.io.BasicTransmittingConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A Basic start point for using the library, as a very crude example.
 */
public class MainTest {

    private BasicTransmittingConnector connector;
    //AX25InputStream

    public static void main(String[] args) {

        System.out.println("Starting");
        MainTest mainTest = new MainTest();
        mainTest.start();


    }


    public void start() {
        try {
            // Connect to kiiss port on direwolf

            System.out.println("Connecting to kiss port");
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 8001);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            System.out.println("Connected to kiss port");



            connector = new BasicTransmittingConnector(in, out, new ConnectionRequestListener() {
                @Override
                public boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port) {
                    System.out.println("Incoming connection from: " + originator.toString());

                    // If we're going to accept then add a listener so we can keep track of the connection
                    state.listener = new ConnectionEstablishmentListener() {
                        @Override
                        public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
System.out.println("CON STATE:" + conn.toString());

                            Thread tx = new Thread(() -> {

                                // Do inputty and outputty stream stuff here
                                try {

                                    InputStream in = state.getInputStream();
                                    Thread t = new Thread(() -> {
                                        System.out.println("RX start");

                                        while(state.isOpen()) {
                                            try {
                                                System.out.println("IN:"+in.read());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        System.out.println("RX finished");
                                    });
                                    t.start();



//
                                    OutputStream out = state.getOutputStream();
                                    out.write("You have connected1!\r".getBytes());
                                    out.flush();
                                    try {Thread.sleep(1000); } catch(InterruptedException e) { }
                                    System.out.println("CON STATE:" + conn.toString());


//                                    out.write("You have connected2!\r".getBytes());
//                                    out.flush();
//
//                                    try {Thread.sleep(1000); } catch(InterruptedException e) { }
//
//                                    out.write("You have connected3!\r".getBytes());
//                                    out.flush();
//
//                                    try {Thread.sleep(1000); } catch(InterruptedException e) { }
//
//                                    out.write("You have connected4!\r".getBytes());
//                                    out.flush();

                                    // Disconnect!
                                    //   try {Thread.sleep(1000); } catch(InterruptedException e) { }

                                   //    state.close();
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                            });

                            tx.start();


                        }

                        @Override
                        public void connectionNotEstablished(Object sessionIdentifier, Object reason) {

                        }

                        @Override
                        public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {

                        }

                        @Override
                        public void connectionLost(Object sessionIdentifier, Object reason) {

                        }
                    };
                    return true;
                }


            });







        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }


    }




}
