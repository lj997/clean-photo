package com.cleanphoto.service;

import com.cleanphoto.model.PhotoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GeoLocationService {

    private static final Map<String, CountryBounds> COUNTRY_BOUNDS = new HashMap<>();
    
    static {
        COUNTRY_BOUNDS.put("中国", new CountryBounds(18.0, 54.0, 73.0, 135.0));
        COUNTRY_BOUNDS.put("美国", new CountryBounds(24.0, 49.0, -125.0, -66.0));
        COUNTRY_BOUNDS.put("日本", new CountryBounds(24.0, 46.0, 122.0, 146.0));
        COUNTRY_BOUNDS.put("韩国", new CountryBounds(33.0, 38.0, 124.0, 131.0));
        COUNTRY_BOUNDS.put("英国", new CountryBounds(49.0, 61.0, -8.0, 2.0));
        COUNTRY_BOUNDS.put("法国", new CountryBounds(41.0, 51.0, -5.0, 9.0));
        COUNTRY_BOUNDS.put("德国", new CountryBounds(47.0, 55.0, 5.0, 15.0));
        COUNTRY_BOUNDS.put("澳大利亚", new CountryBounds(-44.0, -10.0, 113.0, 154.0));
        COUNTRY_BOUNDS.put("加拿大", new CountryBounds(41.0, 83.0, -141.0, -52.0));
        COUNTRY_BOUNDS.put("俄罗斯", new CountryBounds(41.0, 82.0, 19.0, 180.0));
    }

    private static class CountryBounds {
        double minLat, maxLat, minLon, maxLon;
        CountryBounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
        boolean contains(double lat, double lon) {
            return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
        }
    }

    public void resolveGeoLocation(PhotoInfo photoInfo) {
        if (photoInfo.getLatitude() == null || photoInfo.getLongitude() == null) {
            return;
        }

        double lat = photoInfo.getLatitude();
        double lon = photoInfo.getLongitude();

        String country = resolveCountry(lat, lon);
        if ("中国".equals(country)) {
            String[] provinceCity = resolveProvinceCityInChina(lat, lon);
            photoInfo.setCountry(country);
            photoInfo.setProvince(provinceCity[0]);
            photoInfo.setCity(provinceCity[1]);
        } else if (country != null) {
            photoInfo.setCountry(country);
            photoInfo.setProvince(extractMajorCity(lat, lon, country));
            photoInfo.setCity(null);
        }
    }

    private String resolveCountry(double lat, double lon) {
        for (Map.Entry<String, CountryBounds> entry : COUNTRY_BOUNDS.entrySet()) {
            if (entry.getValue().contains(lat, lon)) {
                return entry.getKey();
            }
        }
        return "未知国家";
    }

    private String[] resolveProvinceCityInChina(double lat, double lon) {
        if (isInRange(lat, 39.4, 41.1, lon, 115.4, 117.5)) {
            return new String[]{"北京市", "北京市"};
        } else if (isInRange(lat, 30.7, 31.5, lon, 121.1, 121.9)) {
            return new String[]{"上海市", "上海市"};
        } else if (isInRange(lat, 22.5, 22.9, lon, 113.8, 114.4)) {
            return new String[]{"广东省", "深圳市"};
        } else if (isInRange(lat, 23.0, 23.5, lon, 113.1, 113.5)) {
            return new String[]{"广东省", "广州市"};
        } else if (isInRange(lat, 30.5, 30.7, lon, 104.0, 104.2)) {
            return new String[]{"四川省", "成都市"};
        } else if (isInRange(lat, 29.4, 29.6, lon, 106.5, 106.6)) {
            return new String[]{"重庆市", "重庆市"};
        } else if (isInRange(lat, 32.0, 32.2, lon, 118.7, 118.9)) {
            return new String[]{"江苏省", "南京市"};
        } else if (isInRange(lat, 30.2, 30.4, lon, 120.0, 120.3)) {
            return new String[]{"浙江省", "杭州市"};
        } else if (isInRange(lat, 36.0, 36.2, lon, 103.7, 103.9)) {
            return new String[]{"甘肃省", "兰州市"};
        } else if (isInRange(lat, 43.8, 44.0, lon, 125.2, 125.4)) {
            return new String[]{"吉林省", "长春市"};
        } else if (isInRange(lat, 45.6, 45.8, lon, 126.5, 126.7)) {
            return new String[]{"黑龙江省", "哈尔滨市"};
        } else if (isInRange(lat, 38.0, 38.2, lon, 114.4, 114.6)) {
            return new String[]{"河北省", "石家庄市"};
        } else if (isInRange(lat, 34.7, 34.9, lon, 113.6, 113.8)) {
            return new String[]{"河南省", "郑州市"};
        } else if (isInRange(lat, 30.5, 30.7, lon, 114.2, 114.4)) {
            return new String[]{"湖北省", "武汉市"};
        } else if (isInRange(lat, 28.2, 28.3, lon, 112.9, 113.0)) {
            return new String[]{"湖南省", "长沙市"};
        } else if (isInRange(lat, 36.6, 36.8, lon, 117.0, 117.2)) {
            return new String[]{"山东省", "济南市"};
        } else if (isInRange(lat, 26.0, 26.2, lon, 119.2, 119.4)) {
            return new String[]{"福建省", "福州市"};
        } else if (isInRange(lat, 20.0, 20.1, lon, 110.2, 110.4)) {
            return new String[]{"海南省", "海口市"};
        } else if (isInRange(lat, 22.8, 23.0, lon, 108.3, 108.4)) {
            return new String[]{"广西壮族自治区", "南宁市"};
        } else if (isInRange(lat, 25.0, 25.1, lon, 102.7, 102.8)) {
            return new String[]{"云南省", "昆明市"};
        } else if (isInRange(lat, 29.6, 29.7, lon, 91.1, 91.2)) {
            return new String[]{"西藏自治区", "拉萨市"};
        } else if (isInRange(lat, 38.4, 38.5, lon, 106.2, 106.3)) {
            return new String[]{"宁夏回族自治区", "银川市"};
        } else if (isInRange(lat, 43.8, 43.9, lon, 87.6, 87.7)) {
            return new String[]{"新疆维吾尔自治区", "乌鲁木齐市"};
        } else if (isInRange(lat, 40.8, 40.9, lon, 111.7, 111.8)) {
            return new String[]{"内蒙古自治区", "呼和浩特市"};
        } else {
            return new String[]{"未知省市", "未知城市"};
        }
    }

    private String extractMajorCity(double lat, double lon, String country) {
        if ("美国".equals(country)) {
            if (isInRange(lat, 40.5, 41.0, lon, -74.3, -73.7)) return "纽约";
            if (isInRange(lat, 33.7, 34.2, lon, -118.6, -117.9)) return "洛杉矶";
            if (isInRange(lat, 37.3, 37.8, lon, -122.5, -121.7)) return "旧金山";
        } else if ("日本".equals(country)) {
            if (isInRange(lat, 35.5, 35.8, lon, 139.5, 139.9)) return "东京";
            if (isInRange(lat, 34.6, 34.8, lon, 135.4, 135.6)) return "大阪";
        } else if ("英国".equals(country)) {
            if (isInRange(lat, 51.4, 51.6, lon, -0.3, 0.1)) return "伦敦";
        } else if ("法国".equals(country)) {
            if (isInRange(lat, 48.8, 48.9, lon, 2.2, 2.5)) return "巴黎";
        }
        return null;
    }

    private boolean isInRange(double lat, double minLat, double maxLat, 
                              double lon, double minLon, double maxLon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }
}
