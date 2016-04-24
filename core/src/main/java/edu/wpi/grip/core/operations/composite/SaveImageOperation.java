package edu.wpi.grip.core.operations.composite;

import com.google.common.eventbus.EventBus;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.grip.core.Operation;
import edu.wpi.grip.core.sockets.InputSocket;
import edu.wpi.grip.core.sockets.OutputSocket;
import edu.wpi.grip.core.sockets.SocketHints;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

public class SaveImageOperation implements Operation {
    private static final String DEFAULT_PATH = "image {0,date,Y.M.d H.m.s S}.jpg";
    private static final long WRITE_INTERVAL_MILLIS = 0;
    public static final String KEY_SAVE_IMAGE = "shouldSaveImage";
    public static final String KEY_GRIP = "GRIP";
    private static Logger logger = Logger.getLogger(SaveImageOperation.class.getName());

    private long lastWriteTime;

    public SaveImageOperation() {
        lastWriteTime = 0;
    }

    @Override
    public String getName() {
        return "Save Image";
    }

    @Override
    public String getDescription() {
        return "Saves images from the pipeline as files";
    }

    @Override
    public Category getCategory() {
        return Category.MISCELLANEOUS;
    }

    @Override
    public InputSocket<?>[] createInputSockets(EventBus eventBus) {
        return new InputSocket<?>[]{
                new InputSocket<>(eventBus, SocketHints.Inputs.createMatSocketHint("image", false)),
                new InputSocket<>(eventBus, SocketHints.Inputs.createTextSocketHint("path", DEFAULT_PATH))
        };
    }

    @Override
    public OutputSocket<?>[] createOutputSockets(EventBus eventBus) {
        return new OutputSocket<?>[0];
    }

    @Override
    public void perform(InputSocket<?>[] inputs, OutputSocket<?>[] outputs) {
        Mat image = (Mat) inputs[0].getValue().get();
        String pathPattern = (String) inputs[1].getValue().get();

        checkArgument(image != null && !image.empty(), "Input image must not be empty");
        checkArgument(pathPattern != null && !pathPattern.isEmpty(), "Input path must not be empty");

        NetworkTable gripTable = NetworkTable.getTable(KEY_GRIP);
        if (!gripTable.getBoolean(KEY_SAVE_IMAGE, false)) {
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis < lastWriteTime + WRITE_INTERVAL_MILLIS) {
            return;
        }

        String path = MessageFormat.format(pathPattern, new Date());

        boolean result = imwrite(path, image);
        if (!result) {
            logger.warning(String.format("imwrite failed to path %s", path));
        }

        lastWriteTime = currentTimeMillis;
    }
}
