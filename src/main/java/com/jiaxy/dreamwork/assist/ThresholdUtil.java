package com.jiaxy.dreamwork.assist;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Title:<br>
 * Desc:<br>
 * <p>
 * </p>
 *
 * @author tbrun
 *
 * @since 2015/05/16 20:43
 */
public class ThresholdUtil {

    private static final Config config = ConfigFactory.load();


    public static int getTaskNumThreshold(String dream){
        try {
            return config.getInt(dream);
        } catch (Exception e) {
            //TODO just TEST
            return 1;
        }
    }
}
