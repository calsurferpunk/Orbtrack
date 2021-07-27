package com.nikolaiapps.orbtrack;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.util.Random;
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
    @SuppressWarnings("SpellCheckingInspection")
    private static final String RandomChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890[]'<>/?!@#$%^&*()+-";
    private static final int RandomCharsLength = RandomChars.length();

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
    private static SecretKey getAESKey(String storedKey)
    {
        int keyLength = 256;
        int iterationCount = 1000;
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
    private static void init(Context context)
    {
        int index;
        Random rand;
        SharedPreferences settings = getPreferences(context);
        SharedPreferences.Editor writeSettings;
        String storedKey = settings.getString("storedKey", null);
        StringBuilder randString;

        //if no stored key yet
        if(storedKey == null)
        {
            rand = new Random();
            randString = new StringBuilder();
            writeSettings = settings.edit();

            //while less than 20 chars
            while(randString.length() < 20)
            {
                //get next random char
                index = (int)(rand.nextFloat() * RandomCharsLength);
                randString.append(RandomChars.charAt(index));
            }

            //use and save stored key
            storedKey = randString.toString();
            writeSettings.putString("storedKey", storedKey);
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
        init(context);

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

                default:
                case AlgorithmType.DES:
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
    public static String decrypt(Context context, String value)
    {
        Cipher cipher;
        String[] sections;

        //make sure initialized
        init(context);

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

                default:
                case AlgorithmType.DES:
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
}
