package james.dsp.framework;

import android.media.audiofx.AudioEffect;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * Bass boost is an audio effect to boost or amplify low frequencies of the sound. It is comparable
 * to a simple equalizer but limited to one band amplification in the low frequency range.
 * <p>An application creates a BassBoost object to instantiate and control a bass boost engine in
 * the audio framework.
 * <p>The methods, parameter types and units exposed by the BassBoost implementation are directly
 * mapping those defined by the OpenSL ES 1.0.1 Specification (http://www.khronos.org/opensles/)
 * for the SLBassBoostItf interface. Please refer to this specification for more details.
 * <p>To attach the BassBoost to a particular AudioTrack or MediaPlayer, specify the audio session
 * ID of this AudioTrack or MediaPlayer when constructing the BassBoost.
 * <p>NOTE: attaching a BassBoost to the global audio output mix by use of session 0 is deprecated.
 * <p>See {@link android.media.MediaPlayer#getAudioSessionId()} for details on audio sessions.
 * <p>See {@link android.media.audiofx.AudioEffect} class for more details on
 * controlling audio effects.
 */

public class BassBoost extends AudioEffect {

    private final static String TAG = "BassBoost";

    // These constants must be synchronized with those in
    // frameworks/base/include/media/EffectBassBoostApi.h
    /**
     * Is strength parameter supported by bass boost engine. Parameter ID for getParameter().
     */
    public static final int PARAM_STRENGTH_SUPPORTED = 0;
    /**
     * Bass boost effect strength. Parameter ID for
     * {@link android.media.audiofx.BassBoost.OnParameterChangeListener}
     */
    public static final int PARAM_STRENGTH = 1;
    public static final UUID EFFECT_TYPE_BASS_BOOST = UUID.fromString("42b5cbf5-4dd8-4e79-a5fb-cceb2cb54e13");
    /**
     * Bass boost filter type. Paremeter ID for
     * {@link android.media.audiofx.BassBoost.OnParameterChangeListener}
     * @hide
     */
    public static final int PARAM_FILTER_SLOPE = 2;
    /**
     * Bass boost center frequency. Paremeter ID for
     * {@link android.media.audiofx.BassBoost.OnParameterChangeListener}
     * @hide
     */
    public static final int PARAM_CENTER_FREQUENCY = 3;
 
    /**
     * Indicates if strength parameter is supported by the bass boost engine
     */
    private boolean mStrengthSupported = false;

    /**
     * Registered listener for parameter changes.
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
     * @param priority the priority level requested by the application for controlling the BassBoost
     * engine. As the same engine can be shared by several applications, this parameter indicates
     * how much the requesting application needs control of effect parameters. The normal priority
     * is 0, above normal is a positive number, below normal a negative number.
     * @param audioSession system wide unique audio session identifier. The BassBoost will be
     * attached to the MediaPlayer or AudioTrack in the same audio session.
     *
     * @throws java.lang.IllegalStateException
     * @throws java.lang.IllegalArgumentException
     * @throws java.lang.UnsupportedOperationException
     * @throws java.lang.RuntimeException
     */
    public BassBoost(int priority, int audioSession)
    throws IllegalStateException, IllegalArgumentException,
           UnsupportedOperationException, RuntimeException {
        super(EFFECT_TYPE_BASS_BOOST, EFFECT_TYPE_NULL, priority, audioSession);

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
     * Sets the strength of the bass boost effect. If the implementation does not support per mille
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
     * Sets the filter type of the bass boost effect.
     * @param filter name. The valid range for the filter is [0,1]
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @hide
     */
    public void setFilterSlope(short slope)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        try {
            checkStatus(setParameter(PARAM_FILTER_SLOPE, slope));
        } catch(IllegalArgumentException e) {
            // ignore
        }
    }

    /**
     * Gets the current filter type of the effect
     * @return the filter type of the effect. The valid range is [0,1], in Hertz
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @hide
     */
    public short getFilterSlope() {
        try {
            short[] value = new short[1];
            checkStatus(getParameter(PARAM_FILTER_SLOPE, value));
            return value[0];
        } catch(IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * Sets the center frequency of the bass boost effect.
     * @param freq The frequency, in Hz. The valid range for the freq is [20,500]
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @hide
     */
    public void setCenterFrequency(short freq)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        try {
            checkStatus(setParameter(PARAM_CENTER_FREQUENCY, freq));
        } catch(IllegalArgumentException e) {
            // ignore
        }
    }

    /**
     * Gets the current center frequency of the effect
     * @return the center frequency of the effect. The valid range is [20,500], in Hertz
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     * @hide
     */
    public short getCenterFrequency() {
        try {
            short[] value = new short[1];
            checkStatus(getParameter(PARAM_CENTER_FREQUENCY, value));
            return value[0];
        } catch(IllegalArgumentException e) {
            return 55;
        }
    }

    /**
     * The OnParameterChangeListener interface defines a method called by the BassBoost when a
     * parameter value has changed.
     */
    public interface OnParameterChangeListener  {
        /**
         * Method called when a parameter value has changed. The method is called only if the
         * parameter was changed by another application having the control of the same
         * BassBoost engine.
         * @param effect the BassBoost on which the interface is registered.
         * @param status status of the set parameter operation.
         * @param param ID of the modified parameter. See {@link #PARAM_STRENGTH} ...
         * @param value the new parameter value.
         */
        void onParameterChange(BassBoost effect, int status, int param, short value);
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
                    l.onParameterChange(BassBoost.this, status, p, v);
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
     * The Settings class regroups all bass boost parameters. It is used in
     * conjuntion with getProperties() and setProperties() methods to backup and restore
     * all parameters in a single call.
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
            if (!key.equals("BassBoost")) {
                throw new IllegalArgumentException(
                        "invalid settings for BassBoost: " + key);
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
                    "BassBoost"+
                    ";strength="+Short.toString(strength)
                    );
            return str;
        }
    };


    /**
     * Gets the bass boost properties. This method is useful when a snapshot of current
     * bass boost settings must be saved by the application.
     * @return a BassBoost.Settings object containing all current parameters values
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public BassBoost.Settings getProperties()
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        Settings settings = new Settings();
        short[] value = new short[1];
        checkStatus(getParameter(PARAM_STRENGTH, value));
        settings.strength = value[0];
        return settings;
    }

    /**
     * Sets the bass boost properties. This method is useful when bass boost settings have to
     * be applied from a previous backup.
     * @param settings a BassBoost.Settings object containing the properties to apply
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws UnsupportedOperationException
     */
    public void setProperties(BassBoost.Settings settings)
    throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException {
        checkStatus(setParameter(PARAM_STRENGTH, settings.strength));
    }
}
