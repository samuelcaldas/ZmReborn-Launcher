import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
public final class GenerateLauncherIcons {
    private static final int SOURCE_SIZE = 1024;
    private static final Color SLATE = new Color(0x12, 0x1A, 0x21), AMBER =
            new Color(0xF2, 0xB6, 0x4A), EMBER = new Color(0xD9, 0x5C, 0x4F);
    private static final List<Output> OUTPUTS = List.of(
            new Output("docs/branding/zm-reborn-icon-source.png", 1024), new Output("app/src/main/res/drawable-mdpi/ic_launcher.png", 48),
            new Output("app/src/main/res/drawable-hdpi/ic_launcher.png", 72), new Output("app/src/main/res/drawable-xhdpi/ic_launcher.png", 96),
            new Output("app/src/main/res/drawable-xxhdpi/ic_launcher.png", 144), new Output("app/src/main/res/drawable-xxxhdpi/ic_launcher.png", 192));
    public static void main(String[] arguments) throws IOException {
        boolean verify = parse_arguments(arguments);
        Path repo_root = Path.of("").toAbsolutePath().normalize();
        validate_repo_root(repo_root);
        validate_outputs();
        for (Output output : OUTPUTS) {
            byte[] expected_bytes = encode_png(render_icon(output.dimension()));
            Path output_path = repo_root.resolve(output.relative_path());
            process_output(output_path, expected_bytes, verify);
        }
        System.out.println(verify ? "Launcher icons verified." : "Launcher icons generated.");
    }
    private static boolean parse_arguments(String[] arguments) {
        if (arguments.length == 0) {
            return false;
        }
        if (arguments.length == 1 && "--verify".equals(arguments[0])) {
            return true;
        }
        throw new IllegalArgumentException("Usage: GenerateLauncherIcons [--verify]");
    }
    private static void validate_repo_root(Path repo_root) {
        boolean valid_root = Files.isRegularFile(repo_root.resolve("settings.gradle"));
        valid_root &= Files.isDirectory(repo_root.resolve("app/src/main/res"));
        if (!valid_root) throw new IllegalArgumentException("Run from the ZM Reborn repository root.");
    }
    private static void validate_outputs() {
        for (Output output : OUTPUTS) {
            if (output.dimension() <= 0 || output.dimension() > SOURCE_SIZE)
                throw new IllegalStateException("Invalid output dimension: " + output.dimension());
        }
    }
    private static BufferedImage render_icon(int dimension) {
        BufferedImage image = new BufferedImage(dimension, dimension,
                BufferedImage.TYPE_INT_ARGB);
        Shape field = new RoundRectangle2D.Double(64, 64, 896, 896, 184, 184);
        Shape amber_mark = create_amber_mark();
        Shape ember_accent = create_ember_accent();
        double sample_scale = SOURCE_SIZE / (double) dimension;
        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {
                image.setRGB(x, y, sample_pixel(x, y, sample_scale, field,
                        amber_mark, ember_accent));
            }
        }
        return image;
    }
    private static int sample_pixel(int x, int y, double scale, Shape field,
            Shape amber_mark, Shape ember_accent) {
        int[] totals = new int[4];
        collect_samples(totals, x, y, scale, field, amber_mark, ember_accent);
        return pixel_from_totals(totals);
    }
    private static void collect_samples(int[] totals, int x, int y, double scale,
            Shape field, Shape amber_mark, Shape ember_accent) {
        for (int sample_y = 0; sample_y < 4; sample_y++) {
            for (int sample_x = 0; sample_x < 4; sample_x++) {
                double px = (x + (sample_x + 0.5) / 4.0) * scale;
                double py = (y + (sample_y + 0.5) / 4.0) * scale;
                Color color = color_at(px, py, field, amber_mark, ember_accent);
                if (color == null) continue;
                add_color(totals, color);
            }
        }
    }
    private static void add_color(int[] totals, Color color) {
        totals[0] += color.getRed(); totals[1] += color.getGreen();
        totals[2] += color.getBlue(); totals[3]++;
    }
    private static int pixel_from_totals(int[] totals) {
        if (totals[3] == 0) return 0;
        int alpha = rounded_divide(255 * totals[3], 16);
        int red = rounded_divide(totals[0], totals[3]);
        int green = rounded_divide(totals[1], totals[3]);
        int blue = rounded_divide(totals[2], totals[3]);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
    private static Color color_at(double x, double y, Shape field, Shape amber_mark, Shape ember_accent) {
        if (ember_accent.contains(x, y)) return EMBER;
        if (amber_mark.contains(x, y)) return AMBER;
        return field.contains(x, y) ? SLATE : null;
    }
    private static int rounded_divide(int value, int divisor) {
        return (value + divisor / 2) / divisor;
    }
    private static Path2D create_amber_mark() {
        Path2D mark = new Path2D.Double(Path2D.WIND_NON_ZERO);
        add_polygon(mark, 176, 232, 608, 232, 608, 350, 390, 632,
                510, 632, 510, 792, 176, 792, 176, 664, 398, 380, 176, 380);
        add_polygon(mark, 454, 792, 454, 300, 574, 300, 672, 474, 770, 300,
                848, 300, 848, 792, 716, 792, 716, 562, 672, 640, 584, 486,
                584, 792);
        return mark;
    }
    private static Path2D create_ember_accent() {
        Path2D accent = new Path2D.Double();
        add_polygon(accent, 574, 300, 672, 474, 770, 300, 848, 300,
                672, 610, 510, 324, 510, 300);
        return accent;
    }
    private static void add_polygon(Path2D path, double... coordinates) {
        path.moveTo(coordinates[0], coordinates[1]);
        for (int index = 2; index < coordinates.length; index += 2)
            path.lineTo(coordinates[index], coordinates[index + 1]);
        path.closePath();
    }
    private static byte[] encode_png(BufferedImage image) throws IOException {
        byte[] raw_bytes = encode_raw_pixels(image);
        byte[] compressed_bytes = compress_raw_pixels(raw_bytes);
        return create_png(image, compressed_bytes);
    }
    private static byte[] encode_raw_pixels(BufferedImage image) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        for (int y = 0; y < image.getHeight(); y++) {
            raw.write(0);
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                raw.write(pixel >> 16); raw.write(pixel >> 8); raw.write(pixel); raw.write(pixel >> 24);
            }
        }
        return raw.toByteArray();
    }
    private static byte[] compress_raw_pixels(byte[] raw_bytes) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        compressed.write(new byte[] {0x78, 0x01});
        for (int offset = 0; offset < raw_bytes.length; offset += 65535) {
            int length = Math.min(65535, raw_bytes.length - offset);
            compressed.write(offset + length == raw_bytes.length ? 1 : 0); compressed.write(length);
            compressed.write(length >> 8); compressed.write(~length); compressed.write(~length >> 8);
            compressed.write(raw_bytes, offset, length);
        }
        Adler32 adler = new Adler32(); adler.update(raw_bytes);
        write_int(compressed, (int) adler.getValue());
        return compressed.toByteArray();
    }
    private static byte[] create_png(BufferedImage image, byte[] compressed_bytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        write_int(header, image.getWidth()); write_int(header, image.getHeight());
        header.write(new byte[] {8, 6, 0, 0, 0});
        write_chunk(output, "IHDR", header.toByteArray());
        write_chunk(output, "IDAT", compressed_bytes); write_chunk(output, "IEND", new byte[0]);
        return output.toByteArray();
    }
    private static void write_chunk(ByteArrayOutputStream output, String type, byte[] data)
            throws IOException {
        byte[] type_bytes = type.getBytes(StandardCharsets.US_ASCII);
        write_int(output, data.length);
        output.write(type_bytes);
        output.write(data);
        CRC32 crc = new CRC32();
        crc.update(type_bytes);
        crc.update(data);
        write_int(output, (int) crc.getValue());
    }
    private static void write_int(ByteArrayOutputStream output, int value) {
        output.write(value >> 24); output.write(value >> 16); output.write(value >> 8); output.write(value);
    }
    private static void process_output(Path path, byte[] expected_bytes, boolean verify)
            throws IOException {
        if (verify) {
            if (!Files.isRegularFile(path)
                    || !Arrays.equals(expected_bytes, Files.readAllBytes(path))) {
                throw new IllegalStateException("Generated bytes differ: " + path);
            }
            return;
        }
        Files.createDirectories(path.getParent());
        Files.write(path, expected_bytes);
    }
    private record Output(String relative_path, int dimension) {}
}
