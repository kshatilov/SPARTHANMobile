package me.symlab.kirill.sparthanmobile.Utils;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Queue;

public class Utils {
    public static byte[] getStreamCmd() {
        byte command_data = (byte) 0x01;
        byte payload_data = (byte) 3;
        byte emg_mode = (byte) 0x02;
        byte imu_mode = (byte) 0x00;
        byte class_mode = (byte) 0x00;

        return new byte[]{command_data, payload_data, emg_mode, imu_mode, class_mode};
    }

    public static MappedByteBuffer loadModelFile(Activity activity, String modelFile) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public static byte[] FloatArray2ByteArray(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float value : values) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }


    public static ByteBuffer convertT(Queue q, int batchSize, int x, int y) {
        ByteBuffer result = ByteBuffer.allocateDirect(batchSize * y * x * (Float.SIZE / Byte.SIZE));
        result.order(ByteOrder.LITTLE_ENDIAN);
        int size = 0;
        for (int o = 0; o < batchSize; o++) {
            Iterator i = q.iterator();
            while (i.hasNext()) {
                byte converted[] = Utils.FloatArray2ByteArray((float[]) i.next());
                result.put(converted);
                size += converted.length;
                if (size == batchSize * y * x * (Float.SIZE / Byte.SIZE)) {
                    break;
                }
            }
        }
        return result;
    }

    public static ByteBuffer convert(Queue q, int batchSize, int x, int y) {
        synchronized (q) {
            ByteBuffer result = ByteBuffer.allocateDirect(batchSize * y * x * (Float.SIZE / Byte.SIZE));
            result.order(ByteOrder.LITTLE_ENDIAN);
            int size = 0;
            for (int o = 0; o < batchSize; o++) {
                for (int g = 0; g < y; g++) {
                    Iterator i = q.iterator();
                    while (i.hasNext()) {
                        float v[] = (float[]) i.next();
                        result.putFloat(v[g]);
                        size += Float.SIZE / Byte.SIZE;
                        if (size == batchSize * x * y * (Float.SIZE / Byte.SIZE)) {
                            break;
                        }
                    }
                }
            }
            return result;
        }
    }
}
