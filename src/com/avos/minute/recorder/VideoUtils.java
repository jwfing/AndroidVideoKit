package com.avos.minute.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;

public class VideoUtils {
    private static final String TAG = VideoUtils.class.getSimpleName();

    static double[] matrix = new double[] { 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0,
            0.0, 1.0 };

    public static boolean MergeFiles(String speratedDirPath,
            String targetFileName) {
        File videoSourceDirFile = new File(speratedDirPath);
        String[] videoList = videoSourceDirFile.list();
        List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<Track>();
        for (String file : videoList) {
            Log.d(TAG, "source files" + speratedDirPath
                    + File.separator + file);
            try {
                FileChannel fc = new FileInputStream(speratedDirPath
                        + File.separator + file).getChannel();
                Movie movie = MovieCreator.build(fc);
                for (Track t : movie.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {

                        videoTracks.add(t);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        Movie result = new Movie();

        try {
            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks
                        .toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks
                        .toArray(new Track[videoTracks.size()])));
            }
            IsoFile out = new DefaultMp4Builder().build(result);
            FileChannel fc = new RandomAccessFile(
                    String.format(targetFileName), "rw").getChannel();
            Log.d(TAG, "target file:" + targetFileName);
            TrackBox tb = out.getMovieBox().getBoxes(TrackBox.class).get(1);

            TrackHeaderBox tkhd = tb.getTrackHeaderBox();
            double[] b = tb.getTrackHeaderBox().getMatrix();
            tkhd.setMatrix(matrix);

            fc.position(0);
            out.getBox(fc);
            fc.close();
            for (String file : videoList) {
                File TBRFile = new File(speratedDirPath + File.separator + file);
                TBRFile.delete();
            }
            boolean a = videoSourceDirFile.delete();
            Log.d(TAG, "try to delete dir:" + a);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean clearFiles(String speratedDirPath) {
        File videoSourceDirFile = new File(speratedDirPath);
        if (videoSourceDirFile != null
                && videoSourceDirFile.listFiles() != null) {
            File[] videoList = videoSourceDirFile.listFiles();
            for (File video : videoList) {
                video.delete();
            }
            videoSourceDirFile.delete();
        }
        return true;
    }
    
    public static int createSnapshot(String videoFile, int kind, String snapshotFilepath) {
        return 0;
    };
    
    public static int createSnapshot(String videoFile, int width, int height, String snapshotFilepath) {
        return 0;
    }
}
