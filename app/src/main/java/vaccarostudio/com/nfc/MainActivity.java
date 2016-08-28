package vaccarostudio.com.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvParser;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.AtrUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import fr.devnied.bitlib.BytesUtils;
/*
import io.github.binaryfoo.DecodedData;
import io.github.binaryfoo.RootDecoder;
import io.github.binaryfoo.cmdline.DecodedWriter;
*/
public class MainActivity extends AppCompatActivity {

    private NfcAdapter mNfcAdapter;

    private String[][] nfctechfilter = new String[][] { new String[] { NfcA.class.getName() } };
    private PendingIntent nfcintent;


    private LinearLayout llContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llContainer = (LinearLayout) findViewById(R.id.llContainer);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disable", Toast.LENGTH_LONG).show();
        }

        nfcintent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        readAids();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        Toast.makeText(this, "Tag detected", Toast.LENGTH_LONG).show();

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);



        byte[] id = tag.getId();
        String tech="";
        for (int i =0;i<tag.getTechList().length;i++)
            tech+=tag.getTechList()[i].toString()+" ";

        llContainer.removeAllViews();

        this.llContainer.addView(buildTitle("TagId"));
        this.llContainer.addView(buildText(SharedUtils.Byte2Hex(id)));


        this.llContainer.addView(buildTitle("Tech"));
        this.llContainer.addView(buildText(tech));

        readNfcA(tag);
        mTagcomm=IsoDep.get(tag);
        test();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, nfcintent, null, nfctechfilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }


    public void readNfcA(Tag tag){


        try{
            NfcA tagcomm= NfcA.get(tag);
            Log.d("TAG",""+tagcomm.getSak());
            
            tagcomm.connect();
            Short s = tagcomm.getSak();
            byte[] a = tagcomm.getAtqa();
            String atqa = new String(a, Charset.forName("US-ASCII"));

            this.llContainer.addView(buildTitle("Atqa"));
            this.llContainer.addView(buildText(atqa));


            this.llContainer.addView(buildTitle("Sak"));
            this.llContainer.addView(buildText(s.toString()));
            tagcomm.close();
        }
        catch(Exception e){
            Log.d("TAG","Error when reading tag");
        }
    }



    IsoDep mTagcomm;
    vaccarostudio.com.nfc.Provider mProvider = new vaccarostudio.com.nfc.Provider();
    private byte[] lastAts;
    private EmvCard mCard;



    public void test(){
        try {

            mTagcomm.connect();
            lastAts = getAts(mTagcomm);

            mProvider.setmTagCom(mTagcomm);
            EmvParser parser = new EmvParser(mProvider, true);
            mCard = parser.readEmvCard();
            if (mCard != null) {
                mCard.setAtrDescription(extractAtsDescription(lastAts));
            }


            this.llContainer.addView(buildTitle("CardNumber"));
            this.llContainer.addView(buildText(mCard.getCardNumber()));

            this.llContainer.addView(buildTitle("Application"));
            this.llContainer.addView(buildText(mCard.getAid()));

            this.llContainer.addView(buildTitle("ExpireDate"));
            this.llContainer.addView(buildText(mCard.getExpireDate().toLocaleString()));

            this.llContainer.addView(buildTitle("Type"));
            this.llContainer.addView(buildText(mCard.getType().getName()));

            this.llContainer.addView(buildTitle("CardHolder"));
            this.llContainer.addView(buildText(mCard.getHolderFirstname()+" "+mCard.getHolderLastname()));

            this.llContainer.addView(buildTitle("PinTry"));
            this.llContainer.addView(buildText( String.valueOf(mCard.getLeftPinTry())));

            this.llContainer.addView(buildTitle("Track1"));
            this.llContainer.addView(buildText( mCard.track1 ) );

            this.llContainer.addView(buildTitle("Track2"));
            this.llContainer.addView(buildText( mCard.track2 ) );


            mTagcomm.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TextView buildTitle(String text){
        ActionBar.LayoutParams lparams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        TextView tv=new TextView(this);
        tv.setLayoutParams(lparams);
        tv.setText(text);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }
    public TextView buildText(String text){
        ActionBar.LayoutParams lparams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        TextView tv=new TextView(this);
        tv.setSelected(true);
        tv.setTextIsSelectable(true);
        tv.setLayoutParams(lparams);
        tv.setText(text);
        return tv;
    }

    public static final byte [] SELECT_PPSE={
            0x00,(byte)0xA4,0x04,0x00,0x0E,'2','P','A','Y','.','S','Y','S','.','D','D','F','0','1',0x00
    };
    public static final byte [] SELECT_AID={
            0x00,(byte)0xA4,0x04,0x00,0x07,(byte)0xA0,0x00,0x00,0x00,0x04,0x10,0x10,0x00
    };

    public static final byte [] SELECT_AFL={
            (byte)0x80,(byte)0xA8,0x00,0x00,0x02,(byte)0x83,0x00,0x00
    };
    public static final byte [] SELECT_READRECORD={
            (byte)0x00,(byte)0xB2,0x01,0x0C,0x00
    };
    public static final byte [] SELECT_READINFO={
            (byte)0x00,(byte)0xB2,0x01,0x0C,0x00
    };


    public void selectPSE(){
        //https://blog.saush.com/2006/09/08/getting-information-from-an-emv-chip-card/


        byte[] recv ;
        try {

            mTagcomm.connect();

            // Extract application "2PAY.SYS.DDF01"
            Log.d("TAG","Extract application 2PAY.SYS.DDF01");
            Log.d("OUT ", SharedUtils.Byte2Hex(SELECT_PPSE));
            recv = mTagcomm.transceive(SELECT_PPSE);
            Log.d("IN ", SharedUtils.Byte2Hex(recv));

            //parsing AID: 4F 07
            int tlv=indexOf(recv,(byte)0x4F);
            tlv++;
            byte []aid=new byte [recv[tlv]];
            tlv++;
            for (int i=0;i<aid.length;i++){
                aid[i]=recv[tlv+i];
            }


            byte []select_aid=new byte[5+aid.length+1];
            select_aid[0]= 0x00;
            select_aid[1]= (byte)0xA4;
            select_aid[2]= 0x04;
            select_aid[3]= 0x00;
            select_aid[4]= (byte) aid.length;
            for (int j=0;j<aid.length;j++)
                select_aid[5+j]=aid[j];
            select_aid[5+aid.length]=0x00;


//https://github.com/binaryfoo/emv-bertlv
            //List<DecodedData> decoded = new RootDecoder().decode(SharedUtils.Byte2Hex(recv), "EMV", "apdu-sequence");
            //new DecodedWriter(System.out).write(decoded, "");
            //decoded.get(0).getDecodedData();

            // [Step 4] Now that we know the AID, select the application
            Log.d("TAG","[Step 4] Now that we know the AID, select the application");
            Log.d("OUT ", SharedUtils.Byte2Hex(select_aid));
            recv = mTagcomm.transceive(select_aid);
            Log.d("IN ", SharedUtils.Byte2Hex(recv));

            // [Step 5] Send GET PROCESSING OPTIONS command
            Log.d("TAG","[Step 5] Send GET PROCESSING OPTIONS command");
            Log.d("OUT ", SharedUtils.Byte2Hex(SELECT_AFL));
            recv = mTagcomm.transceive(SELECT_AFL);
            Log.d("IN ", SharedUtils.Byte2Hex(recv));

            // [Step 6] Send READ RECORD with 0 to find out where the record is
            Log.d("TAG","[Step 6] Send READ RECORD with 0 to find out where the record is");
            Log.d("OUT ", SharedUtils.Byte2Hex(SELECT_READRECORD));
            recv = mTagcomm.transceive(SELECT_READRECORD);
            Log.d("IN ", SharedUtils.Byte2Hex(recv));


            //[Step 7] Use READ RECORD with the given number of bytes to retrieve the data"
            Log.d("TAG","[Step 7] Use READ RECORD with the given number of bytes to retrieve the data");

            byte []info1=SELECT_READINFO;
            info1[4]=recv[1];
            Log.d("OUT ", SharedUtils.Byte2Hex(info1));
            recv = mTagcomm.transceive(info1);
            Log.d("IN ", SharedUtils.Byte2Hex(recv));


            mTagcomm.close();
        } catch (IOException e) {
            e.printStackTrace();
        }




    }

    int indexOf(byte[] buffer, byte b){
        for (int i=0;i<buffer.length;i++){
            if (buffer[i]==b)
                return i;
        }
        return -1;
    }


    /*protected byte[] transceive(String hexstr) throws IOException {
        String[] hexbytes = hexstr.split("\\s");
        byte[] bytes = new byte[hexbytes.length];
        for (int i = 0; i < hexbytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexbytes[i], 16);
        }
        log("Send: " + SharedUtils.Byte2Hex(bytes));
        byte[] recv = tagcomm.transceive(bytes);
        log("Received: " + SharedUtils.Byte2Hex(recv));
        return recv;
    }*/

    ArrayList<Application > aidsList;

    private void readAids() {
        // https://www.eftlab.com.au/index.php/site-map/knowledge-base/211-emv-aid-rid-pix

        aidsList= new ArrayList<Application>();


        InputStream is = getResources().openRawResource(R.raw.applications);

        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

        String line = "";
        StringTokenizer st = null;
        try {

            boolean first=true;
            while ((line = reader.readLine()) != null) {
                st = new StringTokenizer(line, ";");
                if(first==true){
                    first=false;
                } else {
                    Application obj= new Application ();
                    try {obj.aid=st.nextToken();}catch(Exception e){}
                    try {obj.vendor=st.nextToken();}catch(Exception e){}
                    try {obj.country=st.nextToken();}catch(Exception e){}
                    try {obj.name=st.nextToken();}catch(Exception e){}
                    try {obj.descriptor = st.nextToken();}catch(Exception e){}
                    aidsList.add(obj);

                }
            }
        } catch (IOException e) {

            e.printStackTrace();
        }



    }
    /**
     * Get ATS from isoDep
     *
     * @param pIso
     *            isodep
     * @return ATS byte array
     */
    private byte[] getAts(final IsoDep pIso) {
        byte[] ret = null;
        if (pIso.isConnected()) {
            // Extract ATS from NFC-A
            ret = pIso.getHistoricalBytes();
            if (ret == null) {
                // Extract ATS from NFC-B
                ret = pIso.getHiLayerResponse();
            }
        }
        return ret;
    }
    public Collection<String> extractAtsDescription(final byte[] pAts) {
        return AtrUtils.getDescriptionFromAts(BytesUtils.bytesToString(pAts));
    }



}