package cn.itbat.microsoft.service.impl;


import cn.itbat.microsoft.config.GraphConfiguration;
import cn.itbat.microsoft.model.GraphUser;
import cn.itbat.microsoft.service.GraphService;
import cn.itbat.microsoft.vo.DirectoryRoleVo;
import cn.itbat.microsoft.vo.GraphUserVo;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mjj
 * @date 2020年05月12日 16:31:04
 */
@Slf4j
@Service
public class GraphServiceImpl implements GraphService {
    private static final String SELECT = "id,userPrincipalName,surname,givenName,mailNickname,displayName,accountEnabled,assignedLicenses,createdDateTime,mobilePhone,companyName,usageLocation,streetAddress,city,state,country";

    @Override
    public List<SubscribedSku> getSubscribedSkus(String appName) {
        // GET /subscribedSkus to get authenticated skuId
        return GraphConfiguration.getGraphClient(appName)
                .subscribedSkus()
                .buildRequest()
                .get().getCurrentPage();
    }

    @Override
    public List<Domain> getDomains(String appName) {
        return GraphConfiguration.getGraphClient(appName)
                .domains()
                .buildRequest().top(999)
                .get().getCurrentPage();
    }

    @Override
    public User getUser(String appName) {
        // GET /me to get authenticated user
        return GraphConfiguration.getGraphClient(appName)
                .me()
                .buildRequest().select(SELECT)
                .get();
    }

    @Override
    public User getUser(String appName, String userId) {
        // GET /users to get authenticated users
        return GraphConfiguration.getGraphClient(appName)
                .users(userId)
                .buildRequest().select(SELECT)
                .get();
    }

    @Override
    public List<User> getUsers(String appName) {
        // GET /users to get authenticated users
        UserCollectionPage UserCollectionPage = GraphConfiguration.getGraphClient(appName)
                .users()
                .buildRequest().select(SELECT).top(999)
                .get();
        return this.getNextPage(UserCollectionPage);
    }

    @Override
    public List<User> getUsers(GraphUserVo graphUserVo) {
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        List<String> filters = new ArrayList<>();
        if (StringUtils.isNotBlank(graphUserVo.getDisplayName())) {
            filters.add(String.format("startswith(displayName,'%s')", graphUserVo.getDisplayName()));
        }
        if (StringUtils.isNotBlank(graphUserVo.getMailNickname())) {
            filters.add(String.format("startswith(mailNickname,'%s')", graphUserVo.getMailNickname()));
        }
        if (StringUtils.isNotBlank(graphUserVo.getUserPrincipalName())) {
            filters.add(String.format("startswith(userPrincipalName,'%s')", graphUserVo.getUserPrincipalName()));
        }
        if (graphUserVo.getAccountEnabled() != null) {
            filters.add(String.format("accountEnabled eq %b", graphUserVo.getAccountEnabled()));
        }
        String filter = filters.stream().reduce((s1, s2) -> s1 + " and " + s2).get();
        requestOptions.add(new QueryOption("$filter", filter));
        // GET /users to get authenticated users
        UserCollectionPage UserCollectionPage = GraphConfiguration.getGraphClient(graphUserVo.getAppName())
                .users()
                .buildRequest(requestOptions).select(SELECT).top(graphUserVo.getTop())
                .get();
        return this.getNextPage(UserCollectionPage);
    }

    private List<User> getNextPage(UserCollectionPage UserCollectionPage) {
        List<User> users = new ArrayList<>();
        users.addAll(UserCollectionPage.getCurrentPage());
        while (UserCollectionPage.getCurrentPage() != null && UserCollectionPage.getCurrentPage().size() > 0) {
            if (UserCollectionPage.getNextPage() == null) {
                return users;
            }
            UserCollectionPage = UserCollectionPage.getNextPage().buildRequest().get();
            if (!CollectionUtils.isEmpty(UserCollectionPage.getCurrentPage())) {
                List<User> currentPage = UserCollectionPage.getCurrentPage();
                users.addAll(currentPage);
            }
        }
        return users;
    }


    @Override
    public User createUser(String appName, GraphUser graphUser) {
        User user = new User();
        user.accountEnabled = true;
        user.surname = graphUser.getSurname();
        user.givenName = graphUser.getGivenName();
        user.displayName = graphUser.getDisplayName();
        user.mailNickname = graphUser.getMailNickname();
        user.userPrincipalName = graphUser.getUserPrincipalName();
        user.mobilePhone = graphUser.getMobilePhone();
        user.usageLocation = graphUser.getUsageLocation();
        user.streetAddress = graphUser.getStreetAddress();
        user.city = graphUser.getCity();
        user.country = graphUser.getCountry();
        user.state = graphUser.getState();
        user.companyName = graphUser.getCompanyName();
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.forceChangePasswordNextSignIn = false;
        passwordProfile.password = graphUser.getPassword();
        user.passwordProfile = passwordProfile;
        User userResult = GraphConfiguration.getGraphClient(appName).users()
                .buildRequest()
                .post(user);
        // if skuId is not null, add skuId license
        if (StringUtils.isNotBlank(graphUser.getSkuId())) {
            User license = this.addLicense(appName, user.userPrincipalName, graphUser.getSkuId());
        }
        return userResult;

    }

    @Override
    public User addLicense(String appName, String userId, String skuId) {
        LinkedList<AssignedLicense> addLicensesList = new LinkedList<AssignedLicense>();
        AssignedLicense addLicenses = new AssignedLicense();
        addLicenses.skuId = UUID.fromString(skuId);
        addLicensesList.add(addLicenses);

        UserAssignLicenseParameterSet parameterSet = new UserAssignLicenseParameterSet();
        parameterSet.addLicenses = addLicensesList;
        parameterSet.removeLicenses = new ArrayList<>();
        return GraphConfiguration.getGraphClient(appName).users(userId)
                .assignLicense(parameterSet)
                .buildRequest()
                .post();
    }

    @Override
    public User cancelLicense(String appName, String skuId, String userId) {
        UUID skuIdUUID = UUID.fromString(skuId);

        UserAssignLicenseParameterSet parameterSet = new UserAssignLicenseParameterSet();
        parameterSet.addLicenses = new ArrayList<>();
        parameterSet.removeLicenses = Collections.singletonList(skuIdUUID);

        return GraphConfiguration.getGraphClient(appName).users(userId)
                .assignLicense(parameterSet)
                .buildRequest()
                .post();
    }


    @Override
    public void enableDisableUser(String appName, String userId, Boolean accountEnabled) {
        User user = new User();
        user.accountEnabled = accountEnabled;
        GraphConfiguration.getGraphClient(appName).users(userId)
                .buildRequest()
                .patch(user);
    }

    @Override
    public void resetPassword(String appName, String userId, String password) {
        User user = new User();
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.forceChangePasswordNextSignIn = false;
        passwordProfile.password = password;
        user.passwordProfile = passwordProfile;
        GraphConfiguration.getGraphClient(appName).users(userId)
                .buildRequest()
                .patch(user);
    }

    @Override
    public void updateUser(String appName, GraphUser graphUser) {
        User user = new User();
        BeanUtils.copyProperties(graphUser, user);
        GraphConfiguration.getGraphClient(appName).users(graphUser.getUserId())
                .buildRequest()
                .patch(user);
    }

    @Override
    public void deletedUser(String appName, String userName) {
        GraphConfiguration.getGraphClient(appName)
                .users(userName)
                .buildRequest()
                .delete();
    }

    @Override
    public List<DirectoryRole> listDirectoryRoles(String appName) {
        try {
            DirectoryRoleCollectionPage iDirectoryRoleCollectionPage = GraphConfiguration.getGraphClient(appName)
                    .directoryRoles()
                    .buildRequest()
                    .get();
            return iDirectoryRoleCollectionPage.getCurrentPage();
        } catch (ClientException exception) {
            log.error(exception.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public List<User> listMembersOfADirectoryRole(String appName, String objectId) {
        try {
            UserCollectionPage page = GraphConfiguration.getGraphClient(appName)
                    .directoryRoles(objectId)
                    .membersAsUser()
                    .buildRequest()
                    .get();
            return page.getCurrentPage();
        } catch (ClientException e) {
            log.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public Map<DirectoryRoleVo, Set<String>> directoryRoleToUserNameMap(String appName) {
        // 查询当前有哪些角色，并查询其角色下有哪些用户
        return listDirectoryRoles(appName)
                .stream()
                .collect(Collectors.toMap(DirectoryRoleVo::new, role -> listMembersOfADirectoryRole(appName, role.id)
                        .stream()
                        .map(m -> m.userPrincipalName)
                        .collect(Collectors.toSet())
                ));
    }

    @Override
    public Boolean addDirectoryRoleMember(String appName, String userId, String roleId) {
        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = userId;
        DirectoryObject resp = GraphConfiguration.getGraphClient(appName)
                .directoryRoles("roleTemplateId=" + roleId)
                .members()
                .references()
                .buildRequest()
                .post(directoryObject);
        return userId.equals(resp.id);
    }

}