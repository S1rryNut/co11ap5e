package com.personal.site.content;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ImageUploadController {
    private static final Path UPLOADS = Path.of("/opt/personal-site/uploads");
    private static final int MAX_EDGE = 1920;

    @PostMapping(value = "/admin/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5_000_000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image size");
        BufferedImage source = ImageIO.read(file.getInputStream());
        if (source == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image");
        double scale = Math.min(1d, (double) MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null); graphics.dispose();
        Files.createDirectories(UPLOADS);
        String name = UUID.randomUUID() + ".jpg";
        if (!ImageIO.write(target, "jpg", UPLOADS.resolve(name).toFile())) throw new IOException("JPEG encoder unavailable");
        return Map.of("url", "/api/public/uploads/" + name);
    }

    @GetMapping("/public/uploads/{name:[a-f0-9\\-]+\\.jpg}")
    public ResponseEntity<Resource> image(@PathVariable String name) throws IOException {
        Path file = UPLOADS.resolve(name).normalize();
        if (!file.startsWith(UPLOADS) || !Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(new UrlResource(file.toUri()));
    }
}
