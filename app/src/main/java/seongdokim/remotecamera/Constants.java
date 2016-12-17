package seongdokim.remotecamera;

/**
 * Created by sdlan on 2016-12-16.
 */

public class Constants {
    // Pre-defined WiFi messages
    public static final int REQUEST_STREAMING = 1;
    public static final int REQUEST_DISCONNECT = 9;
    public static final int REQUEST_OK = 98;
    public static final int REQUEST_IDLE = 99;

    // Pre-defined request keys
    public static final String REQUEST_FIELD = "request";
    public static final String REQUEST_FIELD_BYTE = "bytes";
    public static final String REQUEST_FIELD_WIDTH = "width";
    public static final String REQUEST_FIELD_HEIGHT = "height";
    public static final String REQUEST_ACKNOWLEDGE_NAME = "acknowledge";

    // Message types sent from the Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_IMAGE_RECEIVED = 6;
    public static final int MESSAGE_CONNECTED = 7;

    // Key names received from the Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
}
