package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectCircleComment;
import org.ruoyi.ipd.domain.ProjectCirclePost;
import org.ruoyi.ipd.domain.ProjectFollower;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectCircleCommentMapper;
import org.ruoyi.ipd.mapper.ProjectCirclePostMapper;
import org.ruoyi.ipd.mapper.ProjectFollowerMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目协作圈（ZK-D2+ 后端真实现，对齐原型 GET/POST /api/project-circle/*）。
 *
 * <p>语义（原型 closure.mjs L424-428 同构）：
 * <ul>
 *   <li>可见性：超管=全部；其余=项目成员（project_members）或协作圈成员；</li>
 *   <li>管理人（canManage）：超管 / 市场PM 唯一负责人 / 主组组长（双组长语义）；</li>
 *   <li>写边界：项目 ARCHIVED 只读；动态 2-3000 字、评论 2-2000 字、支持 parentId 楼中楼；</li>
 *   <li>通知：加人通知被加人；评论通知动态作者（本人评论不自我通知）。</li>
 * </ul>
 * mention 扫描（@姓名）登记 P1（依赖 persons 全量名字扫描）。
 */
@Service
@RequiredArgsConstructor
public class ProjectCircleService {

    private static final String ST_ARCHIVED = "ARCHIVED";
    private static final String ST_ACTIVE_PERSON = "ACTIVE";
    private static final String ROLE_MARKET_PM = "MARKET_PM";

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectFollowerMapper projectFollowerMapper;
    private final ProjectCirclePostMapper postMapper;
    private final ProjectCircleCommentMapper commentMapper;
    private final PersonMapper personMapper;
    private final ProductGroupMapper productGroupMapper;
    private final NotificationService notificationService;

    /** 协作圈视图：项目 + 成员 + 动态（含评论）+ canManage。 */
    public Map<String, Object> view(Long projectId, IpdActor actor) {
        Project project = requireVisibleProject(projectId, actor);
        boolean canManage = canManage(project, actor);

        List<ProjectFollower> followers = projectFollowerMapper.selectList(
            new LambdaQueryWrapper<ProjectFollower>()
                .eq(ProjectFollower::getProjectId, project.getId())
                .orderByAsc(ProjectFollower::getId));
        Map<Long, Person> people = loadPeople(followers.stream()
            .map(ProjectFollower::getUserId).collect(Collectors.toSet()));

        List<Map<String, Object>> members = new ArrayList<>();
        for (ProjectFollower f : followers) {
            Person p = people.get(f.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", f.getUserId());
            m.put("name", p != null ? p.getName() : null);
            m.put("personType", p != null ? p.getPersonType() : null);
            m.put("level", p != null ? p.getLevel() : null);
            m.put("circleRole", f.getCircleRole());
            m.put("addedAt", f.getCreateTime());
            members.add(m);
        }

        List<ProjectCirclePost> posts = postMapper.selectList(
            new LambdaQueryWrapper<ProjectCirclePost>()
                .eq(ProjectCirclePost::getProjectId, project.getId())
                .orderByDesc(ProjectCirclePost::getId)
                .last("LIMIT 100"));
        Map<Long, Person> authors = loadPeople(posts.stream()
            .map(ProjectCirclePost::getAuthorId).collect(Collectors.toSet()));
        List<ProjectCircleComment> comments = posts.isEmpty() ? List.of() : commentMapper.selectList(
            new LambdaQueryWrapper<ProjectCircleComment>()
                .in(ProjectCircleComment::getPostId,
                    posts.stream().map(ProjectCirclePost::getId).collect(Collectors.toSet()))
                .orderByAsc(ProjectCircleComment::getId));
        Map<Long, Person> commenters = loadPeople(comments.stream()
            .map(ProjectCircleComment::getAuthorId).collect(Collectors.toSet()));
        Map<Long, List<ProjectCircleComment>> byPost = comments.stream()
            .collect(Collectors.groupingBy(ProjectCircleComment::getPostId));

        List<Map<String, Object>> postViews = new ArrayList<>();
        for (ProjectCirclePost post : posts) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", post.getId());
            v.put("authorId", post.getAuthorId());
            v.put("authorName", authors.containsKey(post.getAuthorId())
                ? authors.get(post.getAuthorId()).getName() : null);
            v.put("objectType", post.getObjectType());
            v.put("objectId", post.getObjectId());
            v.put("content", post.getContent());
            v.put("createdAt", post.getCreateTime());
            v.put("commentCount", byPost.getOrDefault(post.getId(), List.of()).size());
            List<Map<String, Object>> commentViews = new ArrayList<>();
            for (ProjectCircleComment c : byPost.getOrDefault(post.getId(), List.of())) {
                Map<String, Object> cv = new LinkedHashMap<>();
                cv.put("id", c.getId());
                cv.put("authorId", c.getAuthorId());
                cv.put("authorName", commenters.containsKey(c.getAuthorId())
                    ? commenters.get(c.getAuthorId()).getName() : null);
                cv.put("parentId", c.getParentId());
                cv.put("content", c.getContent());
                cv.put("createdAt", c.getCreateTime());
                commentViews.add(cv);
            }
            v.put("comments", commentViews);
            postViews.add(v);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", projectBrief(project));
        result.put("members", members);
        result.put("posts", postViews);
        result.put("canManage", canManage);
        result.put("readOnly", ST_ARCHIVED.equals(project.getStatus()));
        return result;
    }

    /** 可加为协作人候选：本组织 ACTIVE 且未在圈内。仅管理人可见。 */
    public List<Map<String, Object>> candidates(Long projectId, IpdActor actor) {
        Project project = requireVisibleProject(projectId, actor);
        requireCanManage(project, actor);
        Set<Long> existing = projectFollowerMapper.selectList(
                new LambdaQueryWrapper<ProjectFollower>().eq(ProjectFollower::getProjectId, project.getId()))
            .stream().map(ProjectFollower::getUserId).collect(Collectors.toSet());
        List<Person> pool = personMapper.selectList(new LambdaQueryWrapper<Person>()
            .eq(Person::getAccountStatus, ST_ACTIVE_PERSON)
            .orderByAsc(Person::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Person p : pool) {
            if (existing.contains(p.getId())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", p.getId());
            m.put("name", p.getName());
            m.put("personType", p.getPersonType());
            m.put("level", p.getLevel());
            result.add(m);
        }
        return result;
    }

    /** 增加协作人（幂等 upsert 圈角色）+ 通知被加人。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addMember(Long projectId, IpdActor actor, Long userId, String circleRole) {
        Project project = requireVisibleProject(projectId, actor);
        requireCanManage(project, actor);
        requireWritable(project);
        String normalized = normalizeCircleRole(circleRole);
        Person target = personMapper.selectById(userId);
        if (target == null || !ST_ACTIVE_PERSON.equals(target.getAccountStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "协作人员不存在或已停用");
        }
        ProjectFollower existing = projectFollowerMapper.selectOne(
            new LambdaQueryWrapper<ProjectFollower>()
                .eq(ProjectFollower::getProjectId, project.getId())
                .eq(ProjectFollower::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setCircleRole(normalized);
            projectFollowerMapper.updateById(existing);
        } else {
            projectFollowerMapper.insert(ProjectFollower.builder()
                .projectId(project.getId()).userId(userId)
                .circleRole(normalized).addedBy(actor.id())
                .build());
        }
        notificationService.publish(userId, "CIRCLE_MEMBER_ADDED", "FYI",
            "PROJECT_CIRCLE", project.getId(),
            "你已加入项目协作圈",
            actor.name() + " 邀请你参与 " + project.getName(),
            "/ipd/circle?project=" + project.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("circleRole", normalized);
        return result;
    }

    /** 发动态（项目可见者均可；ARCHIVED 只读）。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long projectId, IpdActor actor, String content, String objectType, Long objectId) {
        Project project = requireVisibleProject(projectId, actor);
        requireWritable(project);
        String text = requireLength(content, 2, 3000, "动态内容");
        ProjectCirclePost post = ProjectCirclePost.builder()
            .projectId(project.getId()).authorId(actor.id())
            .objectType(blankToNull(objectType)).objectId(objectId)
            .content(text).build();
        postMapper.insert(post);
        return post.getId();
    }

    /** 评论（通知动态作者，本人不自我通知）。 */
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long postId, IpdActor actor, String content, Long parentId) {
        ProjectCirclePost post = postMapper.selectById(postId);
        if (post == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "动态不存在");
        }
        requireVisibleProject(post.getProjectId(), actor);
        Project project = projectMapper.selectById(post.getProjectId());
        requireWritable(project);
        String text = requireLength(content, 2, 2000, "评论内容");
        ProjectCircleComment comment = ProjectCircleComment.builder()
            .postId(postId).authorId(actor.id()).parentId(parentId).content(text).build();
        commentMapper.insert(comment);
        if (!Objects.equals(post.getAuthorId(), actor.id())) {
            notificationService.publish(post.getAuthorId(), "CIRCLE_POST_REPLIED", "FYI",
                "PROJECT_CIRCLE_POST", post.getId(),
                "项目协作圈收到新回复",
                actor.name() + " 回复了你的动态",
                "/ipd/circle?project=" + post.getProjectId() + "&post=" + post.getId());
        }
        return comment.getId();
    }

    /** 可见性：超管全部；其余=项目成员或已在圈内。 */
    private Project requireVisibleProject(Long projectId, IpdActor actor) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在");
        }
        if ("SUPER_ADMIN".equals(actor.role())) {
            return project;
        }
        boolean member = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getPersonId, actor.id())) > 0;
        boolean follower = projectFollowerMapper.selectCount(new LambdaQueryWrapper<ProjectFollower>()
            .eq(ProjectFollower::getProjectId, projectId)
            .eq(ProjectFollower::getUserId, actor.id())) > 0;
        if (!member && !follower) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "项目不存在或无权查看");
        }
        return project;
    }

    /** 管理人：超管 / 市场PM 唯一负责人 / 主组组长（原型 owner+双组长语义映射）。 */
    private boolean canManage(Project project, IpdActor actor) {
        if ("SUPER_ADMIN".equals(actor.role())) {
            return true;
        }
        boolean marketOwner = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, project.getId())
            .eq(ProjectMember::getPersonId, actor.id())
            .eq(ProjectMember::getRole, ROLE_MARKET_PM)) > 0;
        if (marketOwner) {
            return true;
        }
        if (project.getMainGroupId() != null) {
            ProductGroup group = productGroupMapper.selectById(project.getMainGroupId());
            return group != null && Objects.equals(group.getLeaderPersonId(), actor.id());
        }
        return false;
    }

    private void requireCanManage(Project project, IpdActor actor) {
        if (!canManage(project, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有项目负责人、组长或超级管理员可以管理协作圈");
        }
    }

    private void requireWritable(Project project) {
        if (project != null && ST_ARCHIVED.equals(project.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "项目已归档，协作圈只读");
        }
    }

    private Map<Long, Person> loadPeople(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return personMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(Person::getId, p -> p, (a, b) -> a));
    }

    private Map<String, Object> projectBrief(Project project) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", project.getId());
        m.put("code", project.getCode());
        m.put("name", project.getName());
        m.put("status", project.getStatus());
        m.put("currentStage", project.getCurrentStage());
        return m;
    }

    private String normalizeCircleRole(String circleRole) {
        if (circleRole == null || circleRole.isBlank()) {
            return ProjectFollower.ROLE_FOLLOWER;
        }
        return switch (circleRole.toUpperCase()) {
            case "COMMENTER" -> ProjectFollower.ROLE_COMMENTER;
            case "CONTRIBUTOR" -> ProjectFollower.ROLE_CONTRIBUTOR;
            default -> ProjectFollower.ROLE_FOLLOWER;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String requireLength(String value, int min, int max, String label) {
        if (value == null || value.trim().length() < min) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "至少 " + min + " 字");
        }
        if (value.trim().length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "超过 " + max + " 字");
        }
        return value.trim();
    }
}
