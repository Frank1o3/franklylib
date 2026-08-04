package com.frank1o3.franklylib;

public record Vec3(float x, float y, float z) {
    public static final Vec3 ZERO = new Vec3(0.0f, 0.0f, 0.0f);

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 scale(float factor) {
        return new Vec3(x * factor, y * factor, z * factor);
    }

    public Vec3 normalize() {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0f) {
            return ZERO;
        }
        return scale(1.0f / length);
    }

    public float dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vec3 cross(Vec3 other) {
        return new Vec3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }
}
