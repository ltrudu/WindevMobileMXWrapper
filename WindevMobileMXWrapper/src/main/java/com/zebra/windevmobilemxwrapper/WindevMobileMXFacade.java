package com.zebra.windevmobilemxwrapper;

// Android / Java imports
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;

import com.google.gson_custom.Gson;
import com.google.gson_custom.GsonBuilder;
import com.symbol.emdk.EMDKBase;
import com.symbol.emdk.EMDKException;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.StatusListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class WindevMobileMXFacade {

    public interface IAppelProcedureWL
    {
        void appelProcedureWLSS(String param1, String param2);
        void appelProcedureWLSSS(String param1, String param2, String param3);
        void appelProcedureWLSSSS(String param1, String param2, String param3, String param4);
    }

    public interface IActivityRetriever
    {
        Activity getActivity();
    }

    public class ErrorHolder
    {
        // Provides the error type for characteristic-error
        public String sErrorType = "";

        // Provides the parm name for parm-error
        public String sParmName = "";

        // Provides error description
        public String sErrorDescription = "";
    }

    // Membres
    private String TAG = "WindevMobileMXFacade";

    // Interface pour executer les procedures WL
    // Cet objet doit être implémenté dans la collection de procedures WL
    private IAppelProcedureWL mAppelProcedureWL = null;

    // Interface pour récupérer l'activité courante de l'application
    // Cet objet doit être implémenté dans la collection de procédures WL
    private IActivityRetriever mActivityRetriever = null;

    // Procedure WL appelée en cas d'erreur
    private String msErrorCallback = "";

    // Procedure WL appelée en cas de succès
    private String msSuccesCallback = "";

    // Le contenu du profil
    private String msProfileData = "";

    //Declare a variable to store ProfileManager object
    private ProfileManager mProfileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager mEMDKManager = null;

    // An ArrayList that will contains errors if we find some
    private ArrayList<ErrorHolder> mErrors = new ArrayList<>();

    // Provides full error description string
    public String msErrorString = "";

    // Status Listener implementation (ensure that we retrieve the profile manager asynchronously
    StatusListener mStatusListener = new StatusListener() {
        @Override
        public void onStatus(EMDKManager.StatusData statusData, EMDKBase emdkBase) {
            if(statusData.getResult() == EMDKResults.STATUS_CODE.SUCCESS)
            {
                mProfileManager = (ProfileManager)emdkBase;
                if(msSuccesCallback != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSS(msSuccesCallback, "Profile Manager récupéré avec succès");
                    }
                }
            }
            else
            {
                String errorMessage = "Erreur lors de la récupération du profile manager: " + getResultCode(statusData.getResult());
                logMessage(errorMessage);
                if(msErrorCallback != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSS(msErrorCallback, "Exception lors de la récupération du profile manager: ", getResultCode(statusData.getResult()));
                    }
                }
            }
        }
    };

    // EMDKListener implementation
    EMDKListener mEMDKListener = new EMDKListener() {
        @Override
        public void onOpened(EMDKManager emdkManager) {
            mEMDKManager = emdkManager;
            if(mProfileManager == null)
            {
                try {
                    emdkManager.getInstanceAsync(EMDKManager.FEATURE_TYPE.PROFILE, mStatusListener);
                } catch (EMDKException e) {
                    if(msErrorCallback != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSS(msErrorCallback, "Exception lors de la récupération du profile manager: ", e.getMessage());
                        }
                    }
                    logMessage("Erreur lors de la récupération du profile manager: " + e.getMessage());
                }
            }
            else
            {
                if(msSuccesCallback != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSS(msSuccesCallback, "Profile Manager déjà initialisé");
                    }
                }
            }
        }

        @Override
        public void onClosed() {
            if(mProfileManager != null)
            {
                mProfileManager = null;
            }

            //This callback will be issued when the EMDK closes unexpectedly.
            if (mEMDKManager != null) {
                mEMDKManager.release();
                mEMDKManager = null;
            }
        }
    };


    public WindevMobileMXFacade(IAppelProcedureWL aAppelProcedureWLInterface, IActivityRetriever aActivityRetrieverInterface)
    {
        mAppelProcedureWL = aAppelProcedureWLInterface;
        mActivityRetriever = aActivityRetrieverInterface;
    }

    private Activity getActivity()
    {
        if(mActivityRetriever != null)
        {
            return mActivityRetriever.getActivity();
        }
        return null;
    }

    private void logMessage(String message)
    {
        Log.d(TAG, message);
    }

    private String getResultCode(EMDKResults.STATUS_CODE aStatusCode)
    {
        switch (aStatusCode)
        {
            case FAILURE:
                return "FAILURE";
            case NULL_POINTER:
                return "NULL_POINTER";
            case EMPTY_PROFILENAME:
                return "EMPTY_PROFILENAME";
            case EMDK_NOT_OPENED:
                return "EMDK_NOT_OPENED";
            case CHECK_XML:
                return "CHECK_XML";
            case PREVIOUS_REQUEST_IN_PROGRESS:
                return "PREVIOUS_REQUEST_IN_PROGRESS";
            case PROCESSING:
                return "PROCESSING";
            case NO_DATA_LISTENER:
                return "NO_DATA_LISTENER";
            case FEATURE_NOT_READY_TO_USE:
                return "FEATURE_NOT_READY_TO_USE";
            case FEATURE_NOT_SUPPORTED:
                return "FEATURE_NOT_SUPPORTED";
            case UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    public void initialize(final String aCallbackSucces, final String aCallbackError)
    {
        msErrorCallback = aCallbackError;
        msSuccesCallback = aCallbackSucces;

        if(mEMDKManager == null)
        {
            EMDKResults results = null;
            try
            {
                //The EMDKManager object will be created and returned in the callback.
                results = EMDKManager.getEMDKManager(getActivity().getApplicationContext(), mEMDKListener);
            }
            catch(Exception e)
            {
                if(msErrorCallback != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSS(msErrorCallback, "Erreur lors de la recupération de l'EMDKManager", e.getMessage());
                    }
                }
                e.printStackTrace();
                return;
            }

            //Check the return status of EMDKManager object creation.
            if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
                logMessage("La récupération de l'EMDKManager a été un succès");
            }else {
                logMessage("Erreur lors de la récupération de l'EMDKManager");
            }
        }
        else
        {
            // On simule l'ouverture de l'EMDKManager
            mEMDKListener.onOpened(mEMDKManager);
         }
    }

    public void release()
    {
        //Clean up the objects created by EMDK manager
        if (mProfileManager != null)
            mProfileManager = null;

        if (mEMDKManager != null) {
            mEMDKManager.release();
            mEMDKManager = null;
        }
    }

    public void execute(final String fsMxProfile, final String fsProfileName, final String fsCallbackSucces, final String fsCallbackErreur)
    {
        String[] params = new String[1];
        params[0] = fsMxProfile;
        EMDKResults results = mProfileManager.processProfile(fsProfileName, ProfileManager.PROFILE_FLAG.SET, params);

        //Check the return status of processProfile
        if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

            // Get XML response as a String
            String statusXMLResponse = results.getStatusString();

            try {
                // Empty Error Holder Array List if it already exists
                mErrors.clear();

                // Create instance of XML Pull Parser to parse the response
                XmlPullParser parser = Xml.newPullParser();
                // Provide the string response to the String Reader that reads
                // for the parser
                parser.setInput(new StringReader(statusXMLResponse));
                // Call method to parse the response
                parseXML(parser);

                if ( mErrors.size() == 0 ) {

                    logMessage("Le profil a été exécuté avec succès");
                    logMessage("Profil: " + msProfileData);


                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces,fsMxProfile);
                        }
                    }
                }
                else {
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();

                    Gson gson = builder.create();
                    String erreursJSon = gson.toJson(mErrors);

                    logMessage("Profile update failed:\n" + "Erreur lors de l'execution du profil: \n" + erreursJSon + "\nProfil:\n" + fsMxProfile);

                    if(fsCallbackErreur != "")
                    {
                        if(mAppelProcedureWL != null)
                        {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, fsMxProfile, "Erreur lors de l'execution du profil", erreursJSon);
                        }
                    }
                }

            } catch (XmlPullParserException e) {

                if(fsCallbackErreur != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, fsMxProfile, "Exception lors de l'analyse des résultats du profile manager", e.getMessage());
                    }
                }
                logMessage(e.getMessage());
            }
        }
        else if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS)
        {
            logMessage("Le profil a été executé avec succès");
            logMessage("Profil: " + msProfileData);

            if(fsCallbackSucces != "")
            {
                if(mAppelProcedureWL != null) {

                    mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, fsMxProfile);
                }
            }
        }
        else
        {
            if(fsCallbackErreur != "")
            {
                if(mAppelProcedureWL != null) {

                    mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, fsMxProfile, "Erreur lors de l'execution du profil", getResultCode(results.statusCode));
                }
            }
            logMessage("Profile update failed." + getResultCode(results.statusCode) + "\nProfil:\n" + fsMxProfile);
        }
    }

    // Method to parse the XML response using XML Pull Parser
    private void parseXML(XmlPullParser myParser) {
        int event;
        try {
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = myParser.getEventType();
            // An object that will store a temporary error holder if an error characteristic is found
            ErrorHolder tempErrorHolder = null;
            logMessage("XML document");
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = myParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:
                        logMessage("XML Element:<" + myParser.getText()+">");
                        if (name.equals("characteristic-error"))
                        {
                            if(tempErrorHolder == null)
                                tempErrorHolder = new ErrorHolder();
                            tempErrorHolder.sErrorType = myParser.getAttributeValue(null, "type");
                            if(tempErrorHolder.sParmName != null && TextUtils.isEmpty(tempErrorHolder.sParmName) == false)
                            {
                                msErrorString += "Nom: " + tempErrorHolder.sParmName + "\nType: " + tempErrorHolder.sErrorType + "\nDescription: " + tempErrorHolder.sErrorDescription + ")";
                                mErrors.add(tempErrorHolder);
                                tempErrorHolder = null;
                            }
                        }
                        else if (name.equals("parm-error"))
                        {
                            if(tempErrorHolder == null)
                                tempErrorHolder = new ErrorHolder();
                            tempErrorHolder.sParmName = myParser.getAttributeValue(null, "name");
                            tempErrorHolder.sErrorDescription = myParser.getAttributeValue(null, "desc");
                            if(tempErrorHolder.sErrorType != null && TextUtils.isEmpty(tempErrorHolder.sErrorType) == false)
                            {
                                msErrorString += "Nom: " + tempErrorHolder.sParmName + "\nType: " + tempErrorHolder.sErrorType + "\nDescription: " + tempErrorHolder.sErrorDescription + ")";
                                mErrors.add(tempErrorHolder);
                                tempErrorHolder = null;
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        logMessage("XML Element:<//" + myParser.getText()+">");
                        break;
                }
                event = myParser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getApplicationSignatureBase64(final String fsCallbackSucces, final String fsCallbackErreur)
    {
        try {
            Activity activity = getActivity();

            if (activity == null) {
                if (fsCallbackErreur != "") {
                    if (mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, "getApplicationSignatureBase64", "Erreur lors de la récupération de l'activité courrante.", "getActivity() a retourné Null");
                    }
                }
                logMessage("getApplicationSignatureBase64 Error, getActivity() returned null.");
                return;
            }

            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);


            // Use custom signature if it has been set by the user
            Signature sig = null;

            // Nope, we will get the first apk signing certificate that we find
            // You can copy/paste this snippet if you want to provide your own
            // certificate
            // TODO: use the following code snippet to extract your custom certificate if necessary
            final Signature[] arrSignatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                arrSignatures = packageInfo.signingInfo.getApkContentsSigners();
            }
            else
            {
                arrSignatures = packageInfo.signatures;
            }
            if (arrSignatures == null || arrSignatures.length == 0) {
                if (fsCallbackErreur != "") {
                    if (mAppelProcedureWL != null) {
                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, "getApplicationSignatureBase64", "Signatures array is empty", "");
                    }
                 }
                logMessage("getApplicationSignatureBase64 Signatures array is empty");
                return;
            }
            sig = arrSignatures[0];
            /*
             * Get the X.509 certificate.
             */
            final byte[] rawCert = sig.toByteArray();

            // Get the certificate as a base64 string
            String encoded = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                encoded = Base64.getEncoder().encodeToString(rawCert);
            }
            else
            {
                encoded = android.util.Base64.encodeToString(rawCert, android.util.Base64.NO_WRAP);
            }

            if (fsCallbackSucces != "") {
                if (mAppelProcedureWL != null) {

                    mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, encoded);
                }
            }
        }
        catch(Exception e)
        {
            if (fsCallbackErreur != "") {
                if (mAppelProcedureWL != null) {

                    mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackErreur, "getApplicationSignatureBase64", "Exception", e.getMessage());
                }
            }
            logMessage("getApplicationSignatureBase64 Exception");
            e.printStackTrace();
        }
    }
}
