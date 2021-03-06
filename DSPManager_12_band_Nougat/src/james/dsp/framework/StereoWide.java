package james.dsp.framework;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * @hide
 */

public class StereoWide extends AudioEffect {

    private final static String TAG = "StereoWide";

    /**
     * Is strength parameter supported by stereowide engine. Parameter ID for getParameter().
     */
    public static final int PARAM_STRENGTH_SUPPORTED = 0;
    /**
     * Stereo widener effect strength. Parameter ID for
     * {@link android.media.audiofx.StereoWide.OnParameterChangeListener}
     */
    public static final int PARAM_STRENGTH = 1;

    public static final UUID EFFECT_TYPE_STEREOWIDE = UUID.fromString("37cc2c00-dddd-11db-8577-0002a5d5c51c");
    /**
     * Indicates if strength parameter is supported by the stereowide engine
     */
    private boolean mStrengthSupported = false;

    /**
     * Registered listener for parameter changes.
     * @hide
     */
    private OnParameterChangeListener mParamListener = null;

    /**
     * Listener used internally to to receive raw parameter change event from AudioEffect super class
     */
    private BaseParameterListener mBaseParamListener = null;

    /**
     * Lock for access to mParamListener
     */
    private final Object mParamListenerLock = new Object();

    /**
     * Class constructor.
     * @param priority the priority level requested by the application for controlling the StereoWide
     * engine. As the same engine can be shared by several applications, this parameter indicates
     * how much the requesting application needs control of effect parameters. The normal priority
     * is 0, above normal is a positive number, below normal a negative number.
     * @param audioSession  system wide unique audio session identifier. The StereoWide will
     * be attached to the MediaPlayer or AudioTrack in the same audio session.
     *
     * @throws java.lang.IllegalStateException
     * @throws java.lang.IllegalArgumentException
     * @throws java.lang.UnsupportedOperationException
     * @throws java.lang.RuntimeException
     */
    public StereoWide(int priority, int audioSession)
    throws IllegalStateException, IllegalArgumentException,
           UnsupportedOperationException, RuntimeException {
        super(EFFECT_TYPE_STEREOWIDE, EFFECT_TYPE_NULL, priority, audioSession);

        if (audioSession == 0) {
       	}

        int[] value = new int[1];
        checkStatus(getParameter(PARAM_STRENGTH_SUPPORTED, value));
        mStrengthSupported = (value[0] != 0);
    }

    /**
     * Indicates whether setting strength is supported. If this method returns false, only one
     * strength is supported and the setStrength() method always rounds to that value.
     * @return true is strength parameter is supported, false otherwise
     */
    public boolean getStrengthSupported() {
       return mStrengthSupported;
    }

    /**
     * Sets the strength of the stereomode effect. If the implementation does not support per mille
     * accuracy for setting the strength, it is allowed to round the given strength to the nearest
     * supported value. You can use the {@link #getRoundedStrength()} method to query the
     * (possibly rounded) value that was actually set.
     * @param strength strength of the effect. The valid range for strength strength is [0, 1000],
     * where 0 per mille designates the mildest effect and 1000 per mille designates the strongest.
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public void setStrength(short strength)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(PARAM_STRENGTH, strength));
    }

    /**
     * Gets the current strength of the effect.
     * @return the strength of the effect. The valid range for strength is [0, 1000], where 0 per
     * mille designates the mildest effect and 1000 per mille the strongest
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public short getRoundedStrength()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        short[] value = new short[1];
        checkStatus(getParameter(PARAM_STRENGTH, value));
        return value[0];
    }

    /**
     * The OnParameterChangeListener interface defines a method called by the StereoWide when a
     * parameter value has changed.
     */
    public interface OnParameterChangeListener  {
        /**
         * Method called when a parameter value has changed. The method is called only if the
         * parameter was changed by another application having the control of the same
         * StereoWide engine.
         * @param effect the StereoWide on which the interface is registered.
         * @param status status of the set parameter operation.
         * @param param ID of the modified parameter. See {@link #PARAM_STRENGTH} ...
         * @param value the new parameter value.
         */
        void onParameterChange(StereoWide effect, int status, int param, short value);
    }

    /**
     * Listener used internally to receive unformatted parameter change events from AudioEffect
     * super class.
     */
    private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
        private BaseParameterListener() {

        }
        public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
            OnParameterChangeListener l = null;

            synchronized (mParamListenerLock) {
                if (mParamListener != null) {
                    l = mParamListener;
                }
            }
            if (l != null) {
                int p = -1;
                short v = -1;

                if (param.length == 4) {
                    p = byteArrayToInt(param, 0);
                }
                if (value.length == 2) {
                    v = byteArrayToShort(value, 0);
                }
                if (p != -1 && v != -1) {
                    l.onParameterChange(StereoWide.this, status, p, v);
                }
            }
        }
    }

    /**
     * Registers an OnParameterChangeListener interface.
     * @param listener OnParameterChangeListener interface registered
     */
    public void setParameterListener(OnParameterChangeListener listener) {
        synchronized (mParamListenerLock) {
            if (mParamListener == null) {
                mParamListener = listener;
                mBaseParamListener = new BaseParameterListener();
                super.setParameterListener(mBaseParamListener);
            }
        }
    }

    /**
     * The Settings class regroups all StereoWide parameters. It is used in
     * conjuntion with getProperties() and setProperties() methods to backup and restore
     * all parameters in a single call.
     * @hide
     */
    public static class Settings {
        public short strength;

        public Settings() {
        }

        /**
         * Settings class constructor from a key=value; pairs formatted string. The string is
         * typically returned by Settings.toString() method.
         * @throws IllegalArgumentException if the string is not correctly formatted.
         */
        public Settings(String settings) {
            StringTokenizer st = new StringTokenizer(settings, "=;");
            int tokens = st.countTokens();
            if (st.countTokens() != 3) {
                throw new IllegalArgumentException("settings: " + settings);
            }
            String key = st.nextToken();
            if (!key.equals("StereoWide")) {
                throw new IllegalArgumentException(
                        "invalid settings for StereoWide: " + key);
            }
            try {
                key = st.nextToken();
                if (!key.equals("strength")) {
                    throw new IllegalArgumentException("invalid key name: " + key);
                }
                strength = Short.parseShort(st.nextToken());
             } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("invalid value for key: " + key);
            }
        }

        @Override
        public String toString() {
            String str = new String (
                    "StereoWide"+
                    ";strength="+Short.toString(strength)
                    );
            return str;
        }
    };


    /**
     * Gets the stereowide properties. This method is useful when a snapshot of current
     * stereowide settings must be saved by the application.
     * @return a StereoWide.Settings object containing all current parameters values
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public StereoWide.Settings getProperties()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        Settings settings = new Settings();
        short[] value = new short[1];
        checkStatus(getParameter(PARAM_STRENGTH, value));
        settings.strength = value[0];
        return settings;
    }

    /**
     * Sets the stereowide properties. This method is useful when stereowide settings have to
     * be applied from a previous backup.
     * @param settings a StereoWide.Settings object containing the properties to apply
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public void setProperties(StereoWide.Settings settings)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(PARAM_STRENGTH, settings.strength));
    }
}
