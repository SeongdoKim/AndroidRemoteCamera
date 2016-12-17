package seongdokim.remotecamera;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Seongdo Kim on 2016-12-16.
 */
public class WiFiConnectDialog extends DialogFragment {

    private EditText inputIpAddress;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_wifi_connect, null);

        // Set IP input box the filers
        inputIpAddress = (EditText) dialogView.findViewById(R.id.editText_IPAddress);
        inputIpAddress.setFilters(new InputFilter[]{new IPInputFiler()});

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView);
        builder.setTitle(R.string.title_input_ip_address);
        builder.setPositiveButton(R.string.btn_caption_connect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Send the positive button event back to the host activity
                Intent intent = getActivity().getIntent();
                intent.putExtra("IPAddress", inputIpAddress.getText().toString());
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        });
        builder.setNegativeButton(R.string.btn_caption_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Send the negative button event back to the host activity
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
            }
        });

        return builder.create();
    }

    public String getIPAddress() {
        return inputIpAddress.getText().toString();
    }

    class IPInputFiler implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                    return "";
                } else {
                    String[] splits = resultingTxt.split("\\.");
                    for (int i=0; i<splits.length; i++) {
                        if (Integer.valueOf(splits[i]) > 255) {
                            return "";
                        }
                    }
                }
            }
            return null;
        }
    }
}
