package voluntariado.servicios;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public final class CodigoQr {
    private CodigoQr() { }

    public static byte[] generarImagen(String codigo, int tamano) {
        if (codigo == null || codigo.isBlank() || tamano < 64) {
            throw new IllegalArgumentException("El codigo y un tamano de al menos 64 son obligatorios");
        }
        try {
            BitMatrix matriz = new QRCodeWriter().encode(codigo, BarcodeFormat.QR_CODE, tamano, tamano);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    public static String leerImagen(BufferedImage imagen) {
        if (imagen == null) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("No se encontro un QR en la imagen", e);
        }
    }

    public static String validarCodigoManual(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.length() > 120 || codigo.contains("\n")) {
            throw new IllegalArgumentException("El codigo manual no es valido");
        }
        return codigo.trim();
    }
}
