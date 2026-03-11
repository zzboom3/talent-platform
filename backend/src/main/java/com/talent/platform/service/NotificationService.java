package com.talent.platform.service;

import com.talent.platform.entity.*;
import com.talent.platform.repository.NotificationRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    public Page<Notification> findByUserId(Long userId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepo.findByUserIdAndReadFalseOrderByCreateTimeDesc(userId, pageable);
        }
        return notificationRepo.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }

    public long countUnread(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationRepo.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepo.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepo.markAllReadByUserId(userId);
    }

    public void notifyApplicationNew(JobApplication app) {
        User receiver = app.getJob().getCompany().getUser();
        String talentName = app.getTalent().getRealName() != null && !app.getTalent().getRealName().isBlank()
                ? app.getTalent().getRealName() : app.getTalent().getUser().getUsername();
        String content = "有新申请啦～人才「" + talentName + "」申请了您发布的岗位「" + app.getJob().getTitle() + "」";
        create(Notification.Type.APPLICATION_NEW, receiver.getId(), "新申请", content, app.getId());
    }

    public void notifyApplicationAccepted(JobApplication app) {
        User receiver = app.getTalent().getUser();
        String content = "恭喜！您申请的岗位「" + app.getJob().getTitle() + "」已通过，快去联系企业吧";
        create(Notification.Type.APPLICATION_ACCEPTED, receiver.getId(), "申请通过", content, app.getId());
    }

    public void notifyApplicationRejected(JobApplication app) {
        User receiver = app.getTalent().getUser();
        String content = "您申请的岗位「" + app.getJob().getTitle() + "」暂未通过，别灰心，继续看看其他机会～";
        create(Notification.Type.APPLICATION_REJECTED, receiver.getId(), "申请结果", content, app.getId());
    }

    public void notifyCompanyPendingAudit(Company company) {
        String content = "有企业待审啦～「" + company.getCompanyName() + "」已提交，快来处理吧";
        for (User admin : userRepo.findByRole(User.Role.ADMIN)) {
            create(Notification.Type.COMPANY_PENDING_AUDIT, admin.getId(), "企业待审核", content, company.getId());
        }
    }

    public void notifyCompanyAuditResult(Company company, boolean approved) {
        User receiver = company.getUser();
        String content = approved
                ? "恭喜，您的企业信息审核已通过！"
                : "很遗憾，审核暂未通过，请按反馈修改后重新提交～";
        create(Notification.Type.COMPANY_AUDIT_RESULT, receiver.getId(), "审核结果", content, company.getId());
    }

    public void notifyJobClosed(Job job, List<JobApplication> applicants) {
        String content = "您申请的岗位「" + job.getTitle() + "」已关闭，可以试试其他机会～";
        for (JobApplication app : applicants) {
            User receiver = app.getTalent().getUser();
            create(Notification.Type.JOB_CLOSED, receiver.getId(), "岗位已关闭", content, job.getId());
        }
    }

    public void notifyJobPublished(Job job) {
        String companyName = job.getCompany() != null ? job.getCompany().getCompanyName() : "某企业";
        String content = "有新岗位啦～「" + companyName + "」发布了「" + job.getTitle() + "」，快去看看吧～";
        for (User talent : userRepo.findByRole(User.Role.TALENT)) {
            create(Notification.Type.JOB_NEW, talent.getId(), "新岗位", content, job.getId());
        }
    }

    public void notifyTalentFeatured(TalentProfile talent) {
        User receiver = talent.getUser();
        String content = "恭喜！您已被选为平台精选人才，更多企业将看到您～";
        create(Notification.Type.TALENT_FEATURED, receiver.getId(), "精选人才", content, talent.getId());
    }

    public void notifyCertificateIssued(User user, String courseName) {
        String content = "恭喜完成学习！《" + courseName + "》结业证书已生成，快去查看吧～";
        create(Notification.Type.CERTIFICATE_ISSUED, user.getId(), "证书已生成", content, null);
    }

    public void notifyNewsPendingReview(int count) {
        if (count <= 0) return;
        String content = "又有新内容啦～" + count + " 条资讯/政策待您审核";
        for (User admin : userRepo.findByRole(User.Role.ADMIN)) {
            create(Notification.Type.NEWS_PENDING_REVIEW, admin.getId(), "待审核", content, null);
        }
    }

    public void notifyWelcome(User user) {
        if (notificationRepo.existsByUserIdAndType(user.getId(), Notification.Type.WELCOME)) return;
        String content = "欢迎使用人才公共服务平台！在这里您可以浏览岗位、管理档案、参与智能匹配等～";
        create(Notification.Type.WELCOME, user.getId(), "欢迎加入", content, null);
    }

    public void notifyNewUserRegister(User newUser) {
        String roleStr = newUser.getRole() == User.Role.TALENT ? "人才" : newUser.getRole() == User.Role.ENTERPRISE ? "企业" : "管理员";
        String content = "有新用户加入！「" + newUser.getUsername() + "」已注册为" + roleStr;
        for (User admin : userRepo.findByRole(User.Role.ADMIN)) {
            create(Notification.Type.NEW_USER_REGISTER, admin.getId(), "新用户", content, newUser.getId());
        }
    }

    private void create(Notification.Type type, Long userId, String title, String content, Long relatedId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setRead(false);
        notificationRepo.save(n);
    }
}
