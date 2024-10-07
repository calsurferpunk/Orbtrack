package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public abstract class Encryptor
{
    //Algorithm types
    private static abstract class AlgorithmType
    {
        static final byte AES = 0;
        static final byte DES = 1;
    }

    //Constants
    private static final String StoredKey = "storedEncryptionKey";
    private static final String OldStoredKey = "storedKey";

    //Variables
    private static byte algorithmType;
    private static String algorithm;
    private static SecretKey key;

    //Gets preferences
    private static SharedPreferences getPreferences(Context context)
    {
        return(context.getSharedPreferences("Encryptor", Context.MODE_PRIVATE));
    }

    //Gets an AES key
    @SuppressWarnings("SpellCheckingInspection")
    private static SecretKey getAESKey(String storedKey)
    {
        int keyLength = 256;
        int iterationCount = 100001;
        PBEKeySpec keySpec;
        SecretKeyFactory keyFactory;
        byte[] salt = new byte[keyLength / 8];

        try
        {
            keySpec = new PBEKeySpec(storedKey.toCharArray(), salt, iterationCount, keyLength);
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return(new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(), "AES"));
        }
        catch(Exception ex)
        {
            return(null);
        }
    }

    //Gets a DES key
    private static SecretKey getDESKey(String storedKey)
    {
        DESKeySpec DESSpec;
        SecretKeyFactory keyFactory;
        byte[] storedKeyArray;

        try
        {
            //noinspection CharsetObjectCanBeUsed
            storedKeyArray = storedKey.getBytes(Globals.Encoding.UTF8);
        }
        catch(Exception ex)
        {
            storedKeyArray = storedKey.getBytes();
        }

        try
        {
            DESSpec = new DESKeySpec(storedKeyArray);
            keyFactory = SecretKeyFactory.getInstance("DES");
            return(keyFactory.generateSecret(DESSpec));
        }
        catch(Exception ex)
        {
            return(null);
        }
    }

    //Initializes keys
    private static void init(Context context, String sourceKey)
    {
        SecureRandom rand;
        SharedPreferences settings = getPreferences(context);
        SharedPreferences.Editor writeSettings;
        String storedKey = settings.getString(sourceKey, null);
        StringBuilder randString;
        byte[] randomBytes;

        //if no stored key yet
        if(storedKey == null)
        {
            rand = new SecureRandom();
            randomBytes = rand.generateSeed(20);
            randString = new StringBuilder();
            writeSettings = settings.edit();

            //go through each byte
            for(byte currentByte : randomBytes)
            {
                //add byte as character
                randString.append((char)currentByte);
            }

            //use and save stored key
            storedKey = randString.toString();
            writeSettings.putString(sourceKey, storedKey);
            writeSettings.apply();
        }

        //if no key yet
        if(key == null)
        {
            //try to get AES key
            key = getAESKey(storedKey);
            if(key != null)
            {
                //use AES
                algorithmType = AlgorithmType.AES;
                algorithm = "AES/CBC/PKCS5Padding";
            }
            else
            {
                //use DES
                key = getDESKey(storedKey);
                algorithmType = AlgorithmType.DES;
                algorithm = "DES";
            }
        }
    }

    //Converts array to base 64 string
    private static String arrayToString64(byte[] value)
    {
        return(Base64.encodeToString(value, Base64.DEFAULT));
    }

    //Converts base 64 string to array
    private static byte[] string64ToArray(String value)
    {
        return(Base64.decode(value, Base64.DEFAULT));
    }

    //Encrypt value
    public static String encrypt(Context context, String value)
    {
        Cipher cipher;
        String header;

        //make sure initialized
        init(context, StoredKey);

        try
        {
            cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            //handle based on algorithm type
            switch(algorithmType)
            {
                case AlgorithmType.AES:
                    //get IV header
                    header = arrayToString64(cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV()) + ":";
                    break;

                case AlgorithmType.DES:
                default:
                    //no header
                    header = "";
                    break;
            }

            //return result
            //noinspection CharsetObjectCanBeUsed
            return(header + arrayToString64(cipher.doFinal(value.getBytes(Globals.Encoding.UTF8))));
        }
        catch(Exception ex)
        {
            //return original value
            return(value);
        }
    }

    //Decrypt value
    private static String decrypt(Context context, String value, String sourceKey)
    {
        Cipher cipher;
        String[] sections;

        //make sure initialized
        init(context, sourceKey);

        try
        {
            //get cipher
            cipher = Cipher.getInstance(algorithm);

            //handle based on algorithm type
            switch(algorithmType)
            {
                case AlgorithmType.AES:
                    //get value and IV from input
                    sections = value.split(":");
                    value = sections[1];

                    //decrypt
                    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(string64ToArray(sections[0])));
                    break;

                case AlgorithmType.DES:
                default:
                    //decrypt
                    cipher.init(Cipher.DECRYPT_MODE, key);
                    break;
            }

            //return result
            //noinspection CharsetObjectCanBeUsed
            return(new String(cipher.doFinal(string64ToArray(value)), Globals.Encoding.UTF8));
        }
        catch(Exception ex)
        {
            //return original value
            return(value);
        }
    }
    public static String decrypt(Context context, String value)
    {
        return(decrypt(context, value, StoredKey));
    }

    //Decrypt value with old key if it exists
    public static String decryptOld(Context context, String value)
    {
        boolean haveOldEncryptionKey = (getPreferences(context).getString(OldStoredKey, null) != null);
        return(haveOldEncryptionKey ? decrypt(context, value, OldStoredKey) : null);
    }
}
