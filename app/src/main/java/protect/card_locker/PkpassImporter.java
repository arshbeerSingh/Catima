package protect.card_locker;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PkpassImporter {
    Context context;

    public PkpassImporter(Context context) {
        this.context = context;
    }

    public boolean isPkpass(String type) {
        return Arrays.asList("application/octet-stream", "application/zip", "application/vnd.apple.pkpass", "application/pkpass", "application/vndapplepkpass", "application/vnd-com.apple.pkpass").contains(type);
    }

    public LoyaltyCard fromURI(Uri uri) throws IOException, JSONException {
        ZipInputStream zipInputStream = new ZipInputStream(context.getContentResolver().openInputStream(uri));

        ZipEntry entry;

        while((entry = zipInputStream.getNextEntry()) != null)
        {
            if(!entry.getName().equals("pass.json"))
            {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for(int c = zipInputStream.read(); c != -1; c = zipInputStream.read())
            {
                sb.append((char) c);
            }

            String readData = sb.toString();

            JSONObject json = new JSONObject(readData);

            String store = json.getString("organizationName");
            String note = json.getString("description");

            // https://developer.apple.com/library/archive/documentation/UserExperience/Reference/PassKit_Bundle/Chapters/TopLevel.html#//apple_ref/doc/uid/TP40012026-CH2-SW1
            // barcodes is the new field
            // barcode is deprecated, but used on old iOS versions, so we do fall back to it
            JSONObject barcode = null;
            JSONArray barcodes = null;

            try
            {
                barcodes = json.getJSONArray("barcodes");
            } catch (JSONException ex) { }

            if(barcodes != null)
            {
                barcode = barcodes.getJSONObject(0);
            }
            else
            {
                barcode = json.getJSONObject("barcode");
            }

            if(barcode == null)
            {
                return null;
            }

            String cardId = barcode.getString("message");
            String barcodeType = barcode.getString("format").substring("PKBarcodeFormat".length());
            if(barcodeType.equals("QR"))
            {
                barcodeType = "QR_CODE";
            }
            return new LoyaltyCard(-1, store, note, cardId, barcodeType, null, null);
        }

        return null;
    }
}