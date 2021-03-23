package com.example.qbot.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.qbot.constants.ActivityStatus;
import com.example.qbot.eo.Activity;
import com.example.qbot.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class BotController {

    private final RestTemplate restTemplate = new RestTemplate();

    private final ActivityService activityService;

    private final ScheduledExecutorService scheduledThreadPool;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, String> stringMap = new HashMap<>() {{
        put("854473150", "许");
        put("753672268", "教授");
        put("1913304078", "丘姥爷");
        put("1594852225", "龙哥");
        put("494415482", "殷局");
        put("251683687", "博哥");
        put("2441806936", "B哥");
    }};

    @Autowired
    public BotController(ActivityService activityService, ScheduledExecutorService scheduledThreadPool) {
        this.activityService = activityService;
        this.scheduledThreadPool = scheduledThreadPool;
    }

    @PostMapping("")
    public void message(@RequestBody Map<String, Object> message) {
        Integer groupIdSLH = 934867776;
        JSONObject jsonObject = new JSONObject(message);
        if ("group".equals(message.get("message_type"))) {
            Integer groupId = (Integer) message.get("group_id");
            if (groupIdSLH.equals(groupId)) {
                Integer qqCode = jsonObject.getInteger("user_id");
                String content = (String) message.get("raw_message");
                if (content == null) {
                    return;
                }
                String url = null;
                if (content.contains("咕咕咕") && content.lastIndexOf("咕") == 2) {
                    if (content.contains("在？") || content.contains("在?")) {
                        String reply;
                        if (qqCode == null) {
                            reply = Math.random() > 0.1 ? "在" : "sb";
                        } else {
                            switch (qqCode) {
                                case 753672268:
                                    reply = "教授牛逼";
                                    break;
                                case 1913304078:
                                    reply = "丘姥爷牛逼";
                                    break;
                                case 1594852225:
                                    reply = "龙哥牛逼";
                                    break;
                                case 494415482:
                                    reply = "殷局牛逼";
                                    break;
                                default:
                                    reply = Math.random() > 0.1 ? "在" : "sb";
                            }
                        }
                        url = "http://127.0.0.1:5700/send_group_msg?group_id=" + groupId + "&message=" + reply;
                    }
                } else if (content.contains("预约开车")) {
                    String[] strings = content.split(" ");
                    String reply;
                    if (strings.length != 3) {
                        reply = "格式错误";
                    } else {
                        try {
                            LocalTime localTime = LocalTime.parse(strings[2]);
                            LocalDateTime localDateTime = localTime.atDate(LocalDate.now());
                            Activity activity = new Activity();
                            activity.setGameName(strings[1]);
                            activity.setAppointmentTime(localDateTime);
                            activity.setCreateTime(LocalDateTime.now());
                            activity.setSponsor(jsonObject.getJSONObject("sender").getString("nickname"));
                            activity.setCarNo((int) (Math.random() * 1000));
                            activity.setStatus(ActivityStatus.PROCESSING);
                            JSONArray array = new JSONArray();
                            array.add(qqCode);
                            activity.setTeam(array.toJSONString());
                            activityService.save(activity);
                            reply = "预约成功";
                            Duration duration = Duration.between(LocalDateTime.now(), localDateTime);
                            scheduledThreadPool.schedule(() -> {
                                Activity activity1 = activityService.getById(activity.getId());
                                if (activity1.getStatus() == ActivityStatus.PROCESSING) {
                                    StringBuffer mess = new StringBuffer("滴滴滴 ");
                                    mess.append(activity1.getGameName()).append(" ");
                                    JSONArray parseArray = JSON.parseArray(activity1.getTeam());
                                    parseArray.forEach(o -> mess.append("[CQ:at,qq=").append(o).append("]"));
                                    String uri = "http://127.0.0.1:5700/send_group_msg?group_id=" + groupId + "&message=" + mess;
                                    restTemplate.postForObject(uri, new HashMap<>(), String.class);
                                }
                            }, duration.getSeconds(), TimeUnit.SECONDS);
                        } catch (RuntimeException e) {
                            reply = "预约失败";
                        }
                    }
                    url = "http://127.0.0.1:5700/send_group_msg?group_id=" + groupId + "&message=" + reply;
                } else if (content.contains("今日车队")) {
                    Activity activity = new Activity();
                    activity.setStatus(ActivityStatus.PROCESSING);
                    LocalDate localDate = LocalDate.now();
                    List<Activity> activities = activityService.list(Wrappers.lambdaQuery(activity).and(
                            wrapper -> wrapper.between(Activity::getAppointmentTime, localDate.atStartOfDay(), localDate.atTime(LocalTime.MAX))));
                    StringBuffer reply = new StringBuffer();
                    if (activities == null || activities.isEmpty()) {
                        reply.append("今日无车");
                    } else {
                        activities.forEach(o -> {
                            reply.append(o.getGameName()).append(" ").append(dateTimeFormatter.format(o.getAppointmentTime())).append(" ").append(o.getCarNo()).append(" ");
                            if (o.getTeam() != null) {
                                reply.append("车队成员:");
                                JSONArray array = JSON.parseArray(o.getTeam());
                                array.forEach(a -> reply.append(stringMap.get(String.valueOf(a))).append(" "));
                            }
                            reply.append("\r\n");
                        });
                    }
                    url = "http://127.0.0.1:5700/send_group_msg";
                    JSONObject request = new JSONObject();
                    JSONObject request1 = new JSONObject();
                    JSONObject request2 = new JSONObject();
                    request2.put("text", reply.toString());
                    request1.put("type", "text");
                    request1.put("data", request2);
                    request.put("group_id", groupId);
                    request.put("message", request1);
                    restTemplate.postForObject(url, request, String.class);
                    return;
                } else if (content.contains("取消预约")) {
                    String reply;
                    try {
                        String no = content.split(" ")[1];
                        Activity activity = new Activity();
                        if (no.matches("^\\d+$")) {
                            activity.setCarNo(Integer.valueOf(no));
                            activity.setStatus(ActivityStatus.PROCESSING);
                            Activity activity1 = activityService.getOne(Wrappers.lambdaQuery(activity));
                            if (activity1 == null) {
                                reply = "无此预约";
                            } else {
                                activity1.setStatus(ActivityStatus.CANCEL);
                                activityService.updateById(activity1);
                                reply = "取消成功";
                            }
                        } else {
                            activity.setGameName(no);
                            activity.setStatus(ActivityStatus.PROCESSING);
                            List<Activity> activities = activityService.list(Wrappers.lambdaQuery(activity));
                            if (activities == null || activities.isEmpty()) {
                                reply = "无此预约";
                            } else {
                                activities.forEach(o -> o.setStatus(ActivityStatus.CANCEL));
                                activityService.updateBatchById(activities);
                                reply = "取消成功";
                            }
                        }
                    } catch (RuntimeException e) {
                        reply = "取消失败";
                    }
                    url = "http://127.0.0.1:5700/send_group_msg?group_id=" + groupId + "&message=" + reply;
                } else if (content.contains("上车")) {
                    String reply;
                    try {
                        String no = content.split(" ")[1];
                        Activity activity = new Activity();
                        if (no.matches("^\\d+$")) {
                            activity.setCarNo(Integer.valueOf(no));
                            activity.setStatus(ActivityStatus.PROCESSING);
                            Activity activity1 = activityService.getOne(Wrappers.lambdaQuery(activity));
                            if (activity1 == null) {
                                reply = "无此车";
                            } else {
                                String originTeam = activity1.getTeam();
                                JSONArray array = JSON.parseArray(originTeam);
                                if (array.contains(qqCode)) {
                                    reply = "已在车上";
                                } else {
                                    array.add(qqCode);
                                    activity1.setTeam(array.toJSONString());
                                    activityService.updateById(activity1);
                                    reply = "上车成功";
                                }
                            }
                        } else {
                            activity.setGameName(no);
                            activity.setStatus(ActivityStatus.PROCESSING);
                            List<Activity> activities = activityService.list(Wrappers.lambdaQuery(activity));
                            if (activities == null || activities.isEmpty()) {
                                reply = "无此车";
                            } else {
                                for (Activity activity1 : activities) {
                                    JSONArray array = JSON.parseArray(activity1.getTeam());
                                    if (!array.contains(qqCode)) {
                                        array.add(qqCode);
                                        activity1.setTeam(array.toJSONString());
                                    }
                                }
                                activityService.updateBatchById(activities);
                                reply = "上车成功";
                            }
                        }
                    } catch (RuntimeException e) {
                        reply = "上车失败";
                    }
                    url = "http://127.0.0.1:5700/send_group_msg?group_id=" + groupId + "&message=" + reply;
                }
                if (url != null) {
                    restTemplate.postForObject(url, new HashMap<>(), String.class);
                }
            }
        }
    }

}
