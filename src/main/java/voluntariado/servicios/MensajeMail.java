package voluntariado.servicios;

public record MensajeMail(String destinatario, String asunto, String cuerpo, byte[] qrPng) {
    public MensajeMail {
        if (destinatario == null || destinatario.isBlank() || asunto == null || asunto.isBlank()
                || cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El mail necesita destinatario, asunto y cuerpo");
        }
        qrPng = qrPng == null ? null : qrPng.clone();
    }

    public MensajeMail(String destinatario, String asunto, String cuerpo) {
        this(destinatario, asunto, cuerpo, null);
    }

    @Override
    public byte[] qrPng() {
        return qrPng == null ? null : qrPng.clone();
    }
}
