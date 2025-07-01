import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Deconvolution {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java Deconvolution <imagem_borrada_path>");
            return;
        }

        String imagePath = args[0];
        BufferedImage input = ImageIO.read(new File(imagePath));
        int width = input.getWidth();
        int height = input.getHeight();

        // Separa canais R, G, B
        float[][] red = new float[height][width];
        float[][] green = new float[height][width];
        float[][] blue = new float[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(input.getRGB(x, y));
                red[y][x] = c.getRed() / 255f;
                green[y][x] = c.getGreen() / 255f;
                blue[y][x] = c.getBlue() / 255f;
            }
        }

        // Kernel Gaussiano (ajustável conforme o tipo de desfoque)
        float[][] psf = gaussianKernel(9, 2.0f);
        float[][] psfFlipped = invertKernel(psf);

        // Aplica Richardson-Lucy por canal
        int iterations = 15;

        Thread[] richardsonLucyThreads = new Thread[3];

        float[][] redRestored = createFromImage(red), 
        greenRestored = createFromImage(green), 
        blueRestored = createFromImage(blue);

        richardsonLucyThreads[0] = new Thread(new RichardsonLucyManager(red, psf, psfFlipped, redRestored, iterations));
        richardsonLucyThreads[1] = new Thread(new RichardsonLucyManager(green, psf, psfFlipped, greenRestored, iterations));
        richardsonLucyThreads[2] = new Thread(new RichardsonLucyManager(blue, psf, psfFlipped, blueRestored, iterations));

        for (Thread thread : richardsonLucyThreads) {
            thread.start();
        }
        for (Thread thread : richardsonLucyThreads) {
            thread.join();
        }

        saveColorImage(redRestored, greenRestored, blueRestored, "restaurada.png");
        System.out.println("Imagem restaurada salva como restaurada.png");
    }

    private static float[][] createFromImage(float[][] image) {
        return new float[image.length][image[0].length];
    }

    // Convolução 2D
    public static float[][] convolve(float[][] image, float[][] kernel) {
        int h = image.length, w = image[0].length;

        float[][] result = new float[h][w];

        Thread[] threads = new Thread[2];

        threads[0] = new Thread(new ConvolveManager(image, kernel, result, 0, h /2 ));
        threads[1] = new Thread(new ConvolveManager(image, kernel, result, h/2, h));

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Erro: " + e.getMessage());
            }
        }
        
        return result;
    }

    // Espelha o kernel horizontal e verticalmente
    public static float[][] invertKernel(float[][] kernel) {
        int h = kernel.length, w = kernel[0].length;
        float[][] result = new float[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                result[y][x] = kernel[h - y - 1][w - x - 1];
        return result;
    }

    // Kernel Gaussiano normalizado
    public static float[][] gaussianKernel(int size, float sigma) {
        float[][] kernel = new float[size][size];
        float mean = size / 2f;
        float sum = 0f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float val = (float) Math.exp(-0.5 * (
                    Math.pow((x - mean) / sigma, 2) +
                    Math.pow((y - mean) / sigma, 2)));
                kernel[y][x] = val;
                sum += val;
            }
        }

        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                kernel[y][x] /= sum;

        return kernel;
    }

    // Salva a imagem RGB em PNG
    public static void saveColorImage(float[][] r, float[][] g, float[][] b, String filename) throws Exception {
        int h = r.length, w = r[0].length;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int red = clampToByte(r[y][x] * 255f);
                int green = clampToByte(g[y][x] * 255f);
                int blue = clampToByte(b[y][x] * 255f);
                Color color = new Color(red, green, blue);
                out.setRGB(x, y, color.getRGB());
            }
        }

        ImageIO.write(out, "png", new File(filename));
    }

    private static int clampToByte(float val) {
        return Math.min(255, Math.max(0, Math.round(val)));
    }
    public static class RichardsonLucyManager implements Runnable {
        private float[][] image;
        private float[][] psf;
        private float[][] psfFlipped;
        private float[][] returnValue;
        private int iterations;
        
        
        public RichardsonLucyManager(
            float[][] image, 
            float[][] psf, 
            float[][] psfFlipped, 
            float[][] returnValue,
            int iterations) {
                this.image = image;
                this.psf = psf;
                this.psfFlipped = psfFlipped;
                this.returnValue = returnValue;
                this.iterations = iterations;
            }
            
        public float[][] getReturnValue() {
            return returnValue;
        }

        @Override
        public void run() {
            // Inicializa com valor constante (pode ser a imagem borrada)
            int h = image.length;
            int w = image[0].length;

            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    returnValue[y][x] = 0.5f;

            for (int it = 0; it < iterations; it++) {
                float[][] returnValueBlurred = convolve(returnValue, psf);
                float[][] ratio = new float[h][w];

                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++) {
                        float eb = returnValueBlurred[y][x];
                        ratio[y][x] = (eb > 1e-6f) ? image[y][x] / eb : 0f;
                    }

                float[][] correction = convolve(ratio, psfFlipped);

                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        returnValue[y][x] *= correction[y][x];
            }
        }    
    }

    public static class ConvolveManager implements Runnable {

        private float[][] image, kernel, result;
        private int begin, end;
        
        public ConvolveManager(float[][] image, float[][] kernel, float[][] result, int begin, int end) {
            this.image = image;
            this.kernel = kernel;
            this.result = result;
            this.begin = begin;
            this.end = end;
        }

        @Override
        public void run() {
        int w = image[0].length;
        int kh = kernel.length, kw = kernel[0].length;
        int kyc = kh / 2, kxc = kw / 2;
        for (int y = begin; y < end; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0f;
                for (int ky = 0; ky < kh; ky++) {
                    for (int kx = 0; kx < kw; kx++) {
                        int iy = y + ky - kyc;
                        int ix = x + kx - kxc;
                        if (iy >= 0 && iy < image.length && ix >= 0 && ix < w) {
                            sum += image[iy][ix] * kernel[ky][kx];
                        }
                    }
                }
                result[y][x] = sum;
            }
        }

    }

    }
}

