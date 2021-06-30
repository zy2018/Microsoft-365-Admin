package cn.itbat.microsoft.service;

import cn.itbat.microsoft.model.GraphUser;
import cn.itbat.microsoft.vo.DirectoryRoleVo;
import cn.itbat.microsoft.vo.GraphUserVo;
import com.microsoft.graph.models.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * graph 的具体操作方法
 *
 * @author mjj
 * @date 2020年05月12日 16:30:29
 */
public interface GraphService {

    /**
     * 获取所有的订阅
     *
     * @param appName appName
     * @return List<SubscribedSku>
     */
    List<SubscribedSku> getSubscribedSkus(String appName);

    /**
     * 获取绑定的域名
     *
     * @param appName appName
     * @return 域名
     */
    List<Domain> getDomains(String appName);

    /**
     * 获取当前用户信息
     *
     * @param appName appName
     * @return User
     */
    User getUser(String appName);

    /**
     * 根据用户id获取用户
     *
     * @param appName appName
     * @param userId  用户id
     * @return User
     */
    User getUser(String appName, String userId);

    /**
     * 获取所有的用户
     *
     * @param appName appName
     * @return IUserCollectionPage
     */
    List<User> getUsers(String appName);


    /**
     * 根据用户id获取用户
     *
     * @param graphUserVo 查询条件
     * @return List<User>
     */
    List<User> getUsers(GraphUserVo graphUserVo);


    /**
     * 创建用户
     *
     * @param appName   appName
     * @param graphUser 用户信息
     * @return User
     */
    User createUser(String appName, GraphUser graphUser);

    /**
     * 添加许可证
     *
     * @param appName appName
     * @param skuId   订阅
     * @param userId  用户
     * @return User
     */
    User addLicense(String appName, String userId, String skuId);


    /**
     * 取消许可证
     *
     * @param appName 组织类型
     * @param skuId   许可证
     * @param userId  用户
     * @return 用户信息
     */
    User cancelLicense(String appName, String skuId, String userId);

    /**
     * 启用禁用账户
     *
     * @param appName        组织类型
     * @param userId         用户
     * @param accountEnabled 启用禁用
     */
    void enableDisableUser(String appName, String userId, Boolean accountEnabled);

    /**
     * 重置密码
     *
     * @param appName  组织类型
     * @param userId   用户
     * @param password 密码
     */
    void resetPassword(String appName, String userId, String password);


    /**
     * 更新用户信息
     *
     * @param appName   组织类型
     * @param graphUser 用户
     */
    void updateUser(String appName, GraphUser graphUser);

    /**
     * 删除用户
     *
     * @param appName  appName
     * @param userName 用户信息
     */
    void deletedUser(String appName, String userName);

    /**
     * 列出当前激活的用户角色
     *
     * @param appName appName
     */
    List<DirectoryRole> listDirectoryRoles(String appName);

    /**
     * 列出用户角色下的用户
     *
     * @param appName  appName
     * @param objectId --both the object ID and template ID are OK.-- Now only object ID is effective.
     */
    List<User> listMembersOfADirectoryRole(String appName, String objectId);

    /**
     * 查询当前有哪些角色，并查询其角色下有哪些用户
     *
     * @param appName appName
     */
    Map<DirectoryRoleVo, Set<String>> directoryRoleToUserNameMap(String appName);

    /**
     * 增加用户角色
     *
     * @param appName 组织类型
     * @param userId 用户id
     * @param roleId 角色id
     * @return 结果
     */
    Boolean addDirectoryRoleMember(String appName, String userId, String roleId);

}
