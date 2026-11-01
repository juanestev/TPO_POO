package voluntariado.seguridad;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import voluntariado.modelo.DatosInvalidosException;

// Convierte contrasenas en hashes. Nunca se guarda la contrasena: solo el hash y su sal.
// PBKDF2 es lento a proposito (muchas vueltas), asi adivinar claves por fuerza bruta cuesta mucho.
public final class ServicioClaves {

    public static final int LARGO_MINIMO = 8;
    private static final int VUELTAS = 120_000;
    private static final int BITS_HASH = 256;
    private static final int BYTES_SAL = 16;

    private final SecureRandom azar = new SecureRandom();

    // La sal es un texto aleatorio distinto por usuario: dos usuarios con la misma clave tienen hashes distintos.
    public String generarSal() {
        byte[] sal = new byte[BYTES_SAL];
        azar.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal);
    }

    public String calcularHash(String clave, String sal) {
        if (clave == null || sal == null || sal.isBlank()) {
            throw new DatosInvalidosException("La clave y la sal son obligatorias");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), Base64.getDecoder().decode(sal), VUELTAS, BITS_HASH);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave", e);
        }
    }

    public boolean coincide(String clave, String sal, String hashGuardado) {
        // isEqual tarda lo mismo aunque difieran en el primer caracter: no filtra informacion por el tiempo.
        return MessageDigest.isEqual(calcularHash(clave, sal).getBytes(), hashGuardado.getBytes());
    }

    // Reglas minimas para una clave nueva. Lanza DatosInvalidosException con el motivo.
    public void validarClaveNueva(String clave) {
        if (clave == null || clave.length() < LARGO_MINIMO) {
            throw new DatosInvalidosException("La clave debe tener al menos " + LARGO_MINIMO + " caracteres");
        }
        boolean hayLetra = clave.chars().anyMatch(Character::isLetter);
        boolean hayNumero = clave.chars().anyMatch(Character::isDigit);
        if (!hayLetra || !hayNumero) {
            throw new DatosInvalidosException("La clave debe tener letras y numeros");
        }
    }
}
