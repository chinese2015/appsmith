import React, { useState } from 'react';
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs';
import 'react-tabs/style/react-tabs.css';

// 定义 User, Role, Permission 接口
interface User {
  id: number;
  name: string;
}

interface Role {
  id: number;
  name: string;
}

interface Permission {
  id: number;
  name: string;
}

export const UserRoleAssignment: React.FC = () => {
  const [selectedRoles, setSelectedRoles] = useState<{ [key: number]: number[] }>({});
  const [selectedPermissions, setSelectedPermissions] = useState<{ [key: number]: number[] }>({});

  // 硬编码用户数据
  const users: User[] = [
    { id: 1, name: 'Alice' },
    { id: 2, name: 'Bob' },
    { id: 3, name: 'Charlie' }
  ];

  // 硬编码角色数据
  const roles: Role[] = [
    { id: 1, name: 'Admin' },
    { id: 2, name: 'Editor' },
    { id: 3, name: 'Viewer' }
  ];

  // 硬编码权限数据
  const permissions: Permission[] = [
    { id: 1, name: 'Read' },
    { id: 2, name: 'Write' },
    { id: 3, name: 'Execute' }
  ];

  const handleRoleChange = (userId: number, roleId: number) => {
    setSelectedRoles(prevSelectedRoles => {
      const roles = prevSelectedRoles[userId] || [];
      if (roles.includes(roleId)) {
        return { ...prevSelectedRoles, [userId]: roles.filter(id => id !== roleId) };
      } else {
        return { ...prevSelectedRoles, [userId]: [...roles, roleId] };
      }
    });
  };

  const handlePermissionChange = (roleId: number, permissionId: number) => {
    setSelectedPermissions(prevSelectedPermissions => {
      const permissions = prevSelectedPermissions[roleId] || [];
      if (permissions.includes(permissionId)) {
        return { ...prevSelectedPermissions, [roleId]: permissions.filter(id => id !== permissionId) };
      } else {
        return { ...prevSelectedPermissions, [roleId]: [...permissions, permissionId] };
      }
    });
  };

  const handleSubmit = () => {
    console.log('Assigned Roles:', selectedRoles);
    console.log('Assigned Permissions:', selectedPermissions);
  };

  return (
    <div>
      <h1>RBAC Configuration</h1>
      <Tabs>
        <TabList>
          <Tab>User-Role Mapping</Tab>
          <Tab>Role-Permission Mapping</Tab>
        </TabList>

        <TabPanel>
          <h2>Assign Roles to Users</h2>
          <table>
            <thead>
              <tr>
                <th>User</th>
                <th>Roles</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>
                    {roles.map(role => (
                      <label key={role.id}>
                        <input
                          type="checkbox"
                          checked={selectedRoles[user.id]?.includes(role.id) || false}
                          onChange={() => handleRoleChange(user.id, role.id)}
                        />
                        {role.name}
                      </label>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TabPanel>

        <TabPanel>
          <h2>Assign Permissions to Roles</h2>
          <table>
            <thead>
              <tr>
                <th>Role</th>
                <th>Permissions</th>
              </tr>
            </thead>
            <tbody>
              {roles.map(role => (
                <tr key={role.id}>
                  <td>{role.name}</td>
                  <td>
                    {permissions.map(permission => (
                      <label key={permission.id}>
                        <input
                          type="checkbox"
                          checked={selectedPermissions[role.id]?.includes(permission.id) || false}
                          onChange={() => handlePermissionChange(role.id, permission.id)}
                        />
                        {permission.name}
                      </label>
                    ))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </TabPanel>
      </Tabs>
      <button onClick={handleSubmit}>Save Configuration</button>
    </div>
  );
};
