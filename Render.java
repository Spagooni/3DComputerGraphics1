import java.io.*;

public class Render {
    final int width = 1024;
    final int height = 768;
    final float fov = (float)(Math.PI / 2.0);

    class Vec3f {
        float x, y, z;

        public Vec3f(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
        }

        public Vec3f sub(Vec3f v) {
            return new Vec3f(x - v.x, y - v.y, z - v.z);
        }

        public Vec3f add(Vec3f v) {
            return new Vec3f(x + v.x, y + v.y, z + v.z);
        }

        public float dot(Vec3f v) {
            return x * v.x + y * v.y + z * v.z;
        }

        public Vec3f normalize() {
            float length = (float)Math.sqrt(x*x + y*y + z*z);
            return new Vec3f(x/length, y/length, z/length);
        }

        public Vec3f mul(float scalar) {
            return new Vec3f(x * scalar, y * scalar, z * scalar);
        }

        public byte[] toRGBBytes() {
            int r = (int)(255 * Math.max(0, Math.min(1, x)));
            int g = (int)(255 * Math.max(0, Math.min(1, y)));
            int b = (int)(255 * Math.max(0, Math.min(1, z)));
            return new byte[] { (byte)r, (byte)g, (byte)b };
        }
    }

    class Material {
        Vec3f color;
        public Material(Vec3f color) { this.color = color; }
    }

    class Sphere {
        Vec3f center;
        float radius;
        Material material;

        public Sphere(Vec3f center, float radius, Material material) {
            this.center = center;
            this.radius = radius;
            this.material = material;
        }

        boolean rayIntersect(Vec3f orig, Vec3f dir, float[] t0Out) {
            Vec3f L = center.sub(orig);
            float tca = L.dot(dir);
            float d2 = L.dot(L) - tca * tca;
            if (d2 > radius * radius) return false;
            float thc = (float)Math.sqrt(radius * radius - d2);
            float t0 = tca - thc;
            float t1 = tca + thc;
            if (t0 < 0) t0 = t1;
            if (t0 < 0) return false;
            t0Out[0] = t0;
            return true;
        }
    }

    class HitInfo {
        Vec3f point;
        Vec3f normal;
        Material material;
        float distance;

        public HitInfo(Vec3f point, Vec3f normal, Material material, float distance) {
            this.point = point;
            this.normal = normal;
            this.material = material;
            this.distance = distance;
        }
    }

    HitInfo sceneIntersect(Vec3f orig, Vec3f dir, Sphere[] spheres) {
        float spheresDist = Float.MAX_VALUE;
        HitInfo hitInfo = null;

        for (Sphere sphere : spheres) {
            float[] dist = new float[1];
            if (sphere.rayIntersect(orig, dir, dist) && dist[0] < spheresDist) {
                spheresDist = dist[0];
                Vec3f hit = orig.add(dir.mul(dist[0]));
                Vec3f N = hit.sub(sphere.center).normalize();
                hitInfo = new HitInfo(hit, N, sphere.material, dist[0]);
            }
        }
        return hitInfo;
    }

    Vec3f castRay(Vec3f orig, Vec3f dir, Sphere[] spheres) {
        HitInfo hit = sceneIntersect(orig, dir, spheres);
        if (hit == null) return new Vec3f(0.2f, 0.7f, 0.8f); // background
        return hit.material.color;
    }

    void render(Sphere[] spheres) {
        Vec3f[] framebuffer = new Vec3f[width * height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float x = (2 * (i + 0.5f) / width - 1) * (float)Math.tan(fov / 2) * width / height;
                float y = -(2 * (j + 0.5f) / height - 1) * (float)Math.tan(fov / 2);
                Vec3f dir = new Vec3f(x, y, -1).normalize();
                framebuffer[i + j * width] = castRay(new Vec3f(0, 0, 0), dir, spheres);
            }
        }
        renderToFile(framebuffer);
    }

    void renderToFile(Vec3f[] framebuffer) {
        try (FileOutputStream fos = new FileOutputStream("out.ppm")) {
            fos.write(("P6\n" + width + " " + height + "\n255\n").getBytes());
            for (Vec3f color : framebuffer) {
                fos.write(color.toRGBBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Render render = new Render();

        Material ivory = render.new Material(render.new Vec3f(0.4f, 0.4f, 0.3f));
        Material red = render.new Material(render.new Vec3f(0.3f, 0.1f, 0.1f));

        Sphere[] spheres = new Sphere[] {
            render.new Sphere(render.new Vec3f(-3, 0, -16), 2, ivory),
            render.new Sphere(render.new Vec3f(-1.0f, -1.5f, -12), 2, red),
            render.new Sphere(render.new Vec3f(1.5f, -0.5f, -18), 3, red),
            render.new Sphere(render.new Vec3f(7, 5, -18), 4, ivory)
        };

        render.render(spheres);
    }
}
