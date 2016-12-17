package seongdokim.remotecamera;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Seongdo Kim
 */
public class ConnectionThread {
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    /**
     * Predefined buffer size to send/receive default network message
     */
    private final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Predefined buffer size to send/receive large network message
     */
    private final int LARGE_SIZE_BUFFER_SIZE = 8192;

    /**
     * A {@link Handler} to communicate with the parent fragment.
     */
    private final Handler mHandler;

    /**
     * The current status of the connection
     */
    private int mState;

    /**
     * An IP Address to send images.
     */
    private String mIPAddress;

    /**
     * The target port to connect.
     */
    private int mPort = 1050;

    /**
     * The maximum time length for waiting until the connection is made
     */
    private int mTimeout = 3000;

    /**
     * A {@link Socket} connecting an application with the IP {@code mIPAddress} and the Port
     * {@code mPort}
     */
    private Socket mSocket;

    /**
     *
     */
    private ConnectThread mConnectThread;

    /**
     *
     */
    private ConnectedThread mConnectedThread;

    /**
     * Image queue
     */
    private LinkedList<ImageData> mImageQueue = new LinkedList<ImageData>();

    /**
     * Mutex for the image queue to synchronize the access to the queue.
     */
    private Semaphore mImageQueueLock = new Semaphore(1);

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public ConnectionThread(Context context, Handler handler) {
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param targetIpAddress The IP address of the device to connect
     */
    public synchronized void connect(String targetIpAddress) {
        Log.d(TAG, "connect to: " + targetIpAddress);

        mIPAddress = targetIpAddress;

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Initialize the thread to connect with the given device
        mConnectThread = new ConnectThread();
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Set the current state of the connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Get the current state of the connection
     * @return An integer defining the current connection state
     */
    public int getState() {
        return mState;
    }

    /**
     * Start the ConnectedThread to begin managing a WiFi connection
     *
     */
    public synchronized void connected(Socket socket) {
        Log.d(TAG, "connected, IP address: " + mIPAddress);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, "Default device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the parent fragment
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect the device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_NONE);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final Socket mSocket;

        public ConnectThread() {
            mSocket = new Socket();
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectThread");

            try {
                InetAddress inetAddress = InetAddress.getByName(mIPAddress);
                SocketAddress socketAddress = new InetSocketAddress(inetAddress, mPort);
                mSocket.connect(socketAddress, mTimeout);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Invalid IP address", e);
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Unable to connect the device with IP and port number of: "
                        + mIPAddress + ":" + mPort);
                e.printStackTrace();
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "unable to close() socket during connection failure", e1);
                }
                connectionFailed();
                return;
            }

            if (!mSocket.isConnected() || mSocket.isClosed()) {
                connectionFailed();
            }

            // Start the connected thread
            connected(mSocket);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final Socket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        /**
         * Request Queue
         */
        private LinkedList<Integer> mRequestQueue = new LinkedList<Integer>();

        /**
         * Request queue lock to synchronize the process on request queue.
         */
        private Object requestLock = new Object();

        public ConnectedThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket does not created: " + e.getMessage(), e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnectedThread");
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            byte[] largeBuffer = new byte[LARGE_SIZE_BUFFER_SIZE];
            int bytes, remainingBytes = -1;

            mHandler.obtainMessage(Constants.MESSAGE_CONNECTED).sendToTarget();
            pushRequest(Constants.REQUEST_STREAMING);

            try {
                bytes = mInStream.read(buffer);

                JSONObject jsonObj = new JSONObject(new String(buffer, 0, bytes));
                Log.i(TAG, "Welcome message: " + jsonObj.getString("welcome"));
            } catch (IOException e) {
                Log.e(TAG, "Failed to get a stream instance: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse the welcome message");
            }

            boolean run = true;
            JSONObject jsonObjSend;

            // Keep listening to the InputStream while connected
            while (run) {
                try {
                    int request_code = popRequest();

                    switch (request_code) {
                        case Constants.REQUEST_STREAMING:
                            ImageData imageData = popImage();
                            jsonObjSend = new JSONObject();
                            try {
                                jsonObjSend.put(Constants.REQUEST_FIELD, Constants.REQUEST_STREAMING);
                                jsonObjSend.put(Constants.REQUEST_FIELD_BYTE, imageData.ImageData.length);
                                jsonObjSend.put(Constants.REQUEST_FIELD_WIDTH, imageData.Width);
                                jsonObjSend.put(Constants.REQUEST_FIELD_HEIGHT, imageData.Height);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            write(jsonObjSend.toString().getBytes());

                            // Receive an acknowledgement
                            bytes = mInStream.read(buffer);
                            try {
                                JSONObject jsonObjReceive = new JSONObject(new String(buffer, 0, bytes));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mOutStream.write(imageData.ImageData);

                            pushRequest(Constants.REQUEST_STREAMING);

                            break;

                        case Constants.REQUEST_DISCONNECT:
                            // Send a streaming request
                            jsonObjSend = new JSONObject();
                            try {
                                jsonObjSend.put(Constants.REQUEST_FIELD, Constants.REQUEST_DISCONNECT);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            write(jsonObjSend.toString().getBytes());
                            resetRequest();
                            run = false;
                            break;

                        default:
                            break;
                    }

                    // Send the obtained bytes to the UI Activity
                    //mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                pushRequest(Constants.REQUEST_DISCONNECT);
                this.join(2000);
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread does not respond, force to stop the thread", e);
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
            }
        }

        /******************************************
         * Network Request Functions
         ******************************************/

        /**
         * Get one request from the request queue
         * @return
         */
        public int popRequest() {
            synchronized (requestLock) {
                while (mRequestQueue.size() == 0) {
                    try {
                        requestLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return -1;
                    }
                }

                return mRequestQueue.poll();
            }
        }

        /**
         * Add one request to the request queue
         * @param request
         */
        public void pushRequest(int request) {
            synchronized (requestLock) {
                mRequestQueue.add(request);
                requestLock.notify();
            }
        }

        /**
         * Remove all requests in the request queue
         */
        public void resetRequest() {
            synchronized (mRequestQueue) {
                mRequestQueue.clear();
            }
        }
    }

    public void pushImage(byte[] raw_data, int width, int height) {
        try {
            if (!mImageQueueLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock the image queue");
            }
            mImageQueue.push(new ImageData(raw_data, width, height));
            mImageQueueLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ImageData popImage() {
        ImageData imageData = null;
        try {
            if (!mImageQueueLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock the image queue");
            }
            imageData = mImageQueue.poll();
            mImageQueueLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return imageData;
    }

    private class ImageData {
        public byte[] ImageData;
        public int Width;
        public int Height;

        ImageData(byte[] data, int width, int height) {
            ImageData = data;
            Width = width;
            Height = height;
        }

    }
}
