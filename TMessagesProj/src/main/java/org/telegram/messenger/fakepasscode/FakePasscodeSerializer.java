package org.telegram.messenger.fakepasscode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.JSR310StringParsableDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.MonthDayDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.OffsetTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.DurationKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.InstantKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.LocalDateKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.LocalDateTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.LocalTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.MonthDayKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.OffsetDateTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.OffsetTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.PeriodKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.YearKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.YearMonthKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZoneIdKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZoneOffsetKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZonedDateTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.MonthDaySerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearMonthSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.YearSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZoneIdSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.key.ZonedDateTimeKeySerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FakePasscodeSerializer {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Ignore {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ToggleSerialization {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface EnabledSerialization {
    }

    public static byte[] serializeEncrypted(FakePasscode passcode, String passcodeString) {
        try {
            byte[] fakePasscodeBytes = getJsonMapper().writeValueAsString(passcode).getBytes("UTF-8");

            byte[] initializationVector = new byte[16];
            Utilities.random.nextBytes(initializationVector);
            byte[] key = MessageDigest.getInstance("MD5").digest(passcodeString.getBytes("UTF-8"));
            byte[] encryptedBytes = encryptBytes(compress(fakePasscodeBytes), initializationVector, key, false);
            byte[] resultBytes = new byte[16 + encryptedBytes.length];
            System.arraycopy(initializationVector, 0, resultBytes, 0, 16);
            System.arraycopy(encryptedBytes, 0, resultBytes, 16, encryptedBytes.length);
            return resultBytes;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static FakePasscode deserializeEncrypted(byte[] encryptedPasscodeData, String passcodeString) {
        try {
            byte[] initializationVector = Arrays.copyOfRange(encryptedPasscodeData, 0, 16);
            byte[] key = MessageDigest.getInstance("MD5").digest(passcodeString.getBytes("UTF-8"));
            byte[] encryptedPasscode = Arrays.copyOfRange(encryptedPasscodeData, 16, encryptedPasscodeData.length);
            byte[] decryptedBytes = encryptBytes(encryptedPasscode, initializationVector, key, true);
            FakePasscode passcode = getJsonMapper().readValue(new String(decompress(decryptedBytes)), FakePasscode.class);
            passcode.passcodeHash = calculateHash(passcodeString, SharedConfig.passcodeSalt);
            return passcode;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String serializePlain(FakePasscode passcode) {
        try {
            return getJsonMapper().writeValueAsString(passcode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] encryptBytes(byte[] data, byte[] initializationVector, byte[] key, boolean isDecrypt) throws Exception {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initializationVector);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(isDecrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
        return cipher.doFinal(data);
    }

    public static String calculateHash(String password, byte[] salt) {
        try {
            byte[] passcodeBytes = password.getBytes("UTF-8");
            byte[] bytes = new byte[32 + passcodeBytes.length];
            System.arraycopy(salt, 0, bytes, 0, 16);
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
            System.arraycopy(salt, 0, bytes, passcodeBytes.length + 16, 16);
            return Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static byte[] compress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DeflaterOutputStream defl = new DeflaterOutputStream(out);
            defl.write(in);
            defl.flush();
            defl.close();

            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] decompress(byte[] in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream infl = new InflaterOutputStream(out);
            infl.write(in);
            infl.flush();
            infl.close();

            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static ObjectMapper jsonMapper = null;
    private static ObjectMapper getJsonMapper() {
        if (jsonMapper != null) {
            return jsonMapper;
        }
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.registerModule(new KotlinModule());
        FakePasscodeSerializerProvider provider = new FakePasscodeSerializerProvider();
        jsonMapper.setSerializerProvider(provider);
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        jsonMapper.setAnnotationIntrospector(new FakePasscodeAnnotationIntrospector());
        jsonMapper.setVisibility(jsonMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return jsonMapper;
    }

    public final static class FakePasscodeAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember m) {
            return m.hasAnnotation(Ignore.class)
                    || m.hasAnnotation(Deprecated.class)
                    || super.hasIgnoreMarker(m);
        }

        @Override
        public Object findSerializationConverter(Annotated a) {
            if (a.getRawType().isAnnotationPresent(ToggleSerialization.class)) {
                return new StdConverter<Object, Boolean>() {
                    @Override
                    public Boolean convert(Object value) {
                        return value != null;
                    }
                };
            }
            if (a.getRawType().isAnnotationPresent(EnabledSerialization.class)) {
                return new StdConverter<Object, Boolean>() {
                    @Override
                    public Boolean convert(Object value) {
                        if (value == null) {
                            return null;
                        }
                        try {
                            Field field = value.getClass().getField("enabled");
                            return field.getBoolean(value);
                        } catch (Exception ignored) {
                            return null;
                        }
                    }
                };
            } else if (a instanceof AnnotatedField && a.getName().equals("mode") && a.getRawType().equals(int.class)) {
                AnnotatedField annotatedField = (AnnotatedField) a;
                Class<?> declaringClass = annotatedField.getAnnotated().getDeclaringClass();
                if (TerminateOtherSessionsAction.class.isAssignableFrom(declaringClass)
                    || CheckedSessions.class.isAssignableFrom(declaringClass)) {
                    return new StdConverter<Integer, String>() {
                        @Override
                        public String convert(Integer value) {
                            if (value == SelectionMode.SELECTED) {
                                return "SELECTED";
                            } else if ((int)value == SelectionMode.EXCEPT_SELECTED) {
                                return "EXCEPT_SELECTED";
                            }
                            return null;
                        }
                    };
                }
            }
            return super.findSerializationConverter(a);
        }

        @Override
        public Object findDeserializationConverter(Annotated a) {
            if (a.getRawType().isAnnotationPresent(ToggleSerialization.class)) {
                return new StdConverter<Boolean, Object>() {
                    @Override
                    public Object convert(Boolean value) {
                        if (value == null || !value) {
                            return null;
                        }
                        try {
                            return a.getRawType().newInstance();
                        } catch (Exception ignored) {
                            return null;
                        }
                    }
                };
            }
            if (a.getRawType().isAnnotationPresent(EnabledSerialization.class)) {
                return new StdConverter<Boolean, Object>() {
                    @Override
                    public Object convert(Boolean value) {
                        if (value == null) {
                            return null;
                        }
                        try {
                            Object instance = a.getRawType().newInstance();
                            Field field = instance.getClass().getField("enabled");
                            field.setBoolean(instance, value);
                            return instance;
                        } catch (Exception ignored) {
                            return null;
                        }
                    }
                };
            } else if (a instanceof AnnotatedField && a.getName().equals("mode") && a.getRawType().equals(int.class)) {
                AnnotatedField annotatedField = (AnnotatedField) a;
                Class<?> declaringClass = annotatedField.getAnnotated().getDeclaringClass();
                if (TerminateOtherSessionsAction.class.isAssignableFrom(declaringClass)
                        || CheckedSessions.class.isAssignableFrom(declaringClass)) {
                    return new StdConverter<String, Integer>() {
                        @Override
                        public Integer convert(String value) {
                            if (value == null) {
                                return null;
                            }
                            if (value.equals("SELECTED")) {
                                return SelectionMode.SELECTED;
                            } else if (value.equals("EXCEPT_SELECTED")) {
                                return SelectionMode.EXCEPT_SELECTED;
                            }
                            return null;
                        }
                    };
                }
            }

            return super.findDeserializationConverter(a);
        }

        @Override
        public String findImplicitPropertyName(AnnotatedMember m) {
            if (m instanceof AnnotatedField && (
                    m.getRawType().isAnnotationPresent(ToggleSerialization.class)
                            || m.getRawType().isAnnotationPresent(EnabledSerialization.class)
            )) {
                AnnotatedField annotatedField = (AnnotatedField) m;
                if (annotatedField.getName().endsWith("Action")) {
                    return annotatedField.getName().replace("Action", "");
                }
            }
            return super.findImplicitPropertyName(m);
        }
    }

    public final static class FakePasscodeSerializerProvider extends DefaultSerializerProvider {
        private static final long serialVersionUID = 1L;

        public FakePasscodeSerializerProvider() { super(); }
        public FakePasscodeSerializerProvider(FakePasscodeSerializerProvider src) { super(src); }

        protected FakePasscodeSerializerProvider(SerializerProvider src, SerializationConfig config,
                       SerializerFactory f) {
            super(src, config, f);
        }

        @Override
        public DefaultSerializerProvider copy()
        {
            if (getClass() != FakePasscodeSerializerProvider.class) {
                return super.copy();
            }
            return new FakePasscodeSerializerProvider(this);
        }

        @Override
        public FakePasscodeSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf) {
            return new FakePasscodeSerializerProvider(this, config, jsf);
        }

        @Override
        public JsonSerializer<Object> findNullValueSerializer(BeanProperty property) {
            if (property.getType().getRawClass().isAnnotationPresent(ToggleSerialization.class)) {
                return new ToggleSerializer();
            }
            return _nullValueSerializer;
        }
    }

    public static class ToggleSerializer extends StdSerializer<Object> {

        public ToggleSerializer() {
            this(null);
        }

        public ToggleSerializer(Class<Object> t) {
            super(t);
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeBoolean(value != null);
        }
    }
}
