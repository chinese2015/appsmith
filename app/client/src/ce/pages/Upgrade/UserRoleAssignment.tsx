import React, { useState } from 'react';
import { Button, Dialog, FormGroup, InputGroup, MenuItem, Tabs, Tab } from '@blueprintjs/core';
import { MultiSelect, Select } from '@blueprintjs/select';

type UserType = {
  id: number;
  name: string;
  roles: RoleType[];
};

type RoleType = {
  id: number;
  name: string;
  permissions: PermissionType[];
};

type PermissionType = {
  id: number;
  name: string;
};

const UserSelect = Select.ofType<UserType>();
const RoleSelect = Select.ofType<RoleType>();

const UserRoleAssignment = () => {
  // 定义用户、角色和权限数据结构
  const [users, setUsers] = useState<UserType[]>([
    { id: 1, name: 'User 1', roles: [] },
    { id: 2, name: 'User 2', roles: [] },
    // 更多用户...
  ]);

  const [roles, setRoles] = useState<RoleType[]>([
    { id: 1, name: 'Role 1', permissions: [] },
    { id: 2, name: 'Role 2', permissions: [] },
    // 更多角色...
  ]);

  const [permissions] = useState<PermissionType[]>([
    { id: 1, name: 'Permission 1' },
    { id: 2, name: 'Permission 2' },
    // 更多权限...
  ]);

  // 定义状态变量
  const [selectedUser, setSelectedUser] = useState<UserType | null>(null);
  const [selectedRole, setSelectedRole] = useState<RoleType | null>(null);
  const [isUserDialogOpen, setIsUserDialogOpen] = useState(false);
  const [isRoleDialogOpen, setIsRoleDialogOpen] = useState(false);
  const [newRoleName, setNewRoleName] = useState('');

  // 打开用户弹出窗口
  const openUserDialog = (user: UserType) => {
    setSelectedUser(user);
    setIsUserDialogOpen(true);
  };

  // 打开角色弹出窗口
  const openRoleDialog = (role: RoleType) => {
    setSelectedRole(role);
    setIsRoleDialogOpen(true);
  };

  // 关闭弹出窗口
  const closeUserDialog = () => setIsUserDialogOpen(false);
  const closeRoleDialog = () => setIsRoleDialogOpen(false);

  // 处理角色选择变化
  const handleRoleChange = (roles: RoleType[]) => {
    if (selectedUser) {
      setSelectedUser({ ...selectedUser, roles });
    }
  };

  // 处理权限选择变化
  const handlePermissionChange = (permissions: PermissionType[]) => {
    if (selectedRole) {
      setSelectedRole({ ...selectedRole, permissions });
    }
  };

  // 处理新角色创建
  const createNewRole = () => {
    const newRole = { id: roles.length + 1, name: newRoleName, permissions: [] };
    setRoles([...roles, newRole]);
    setNewRoleName('');
  };

  return (
    <div>
      <Tabs id="UserRoleTabs">
        <Tab id="users" title="Users" panel={
          <div>
            <h3>Users</h3>
            <UserSelect
              items={users}
              itemRenderer={(user, { handleClick, modifiers }) => (
                <MenuItem
                  key={user.id}
                  text={user.name}
                  onClick={handleClick}
                  active={modifiers.active}
                />
              )}
              noResults={<MenuItem disabled={true} text="No results." />}
              onItemSelect={openUserDialog}
              popoverProps={{ minimal: true }}
            >
              <Button text="Select a user..." rightIcon="double-caret-vertical" />
            </UserSelect>
          </div>
        } />
        <Tab id="roles" title="Roles" panel={
          <div>
            <h3>Roles</h3>
            <RoleSelect
              items={roles}
              itemRenderer={(role, { handleClick, modifiers }) => (
                <MenuItem
                  key={role.id}
                  text={role.name}
                  onClick={handleClick}
                  active={modifiers.active}
                />
              )}
              noResults={<MenuItem disabled={true} text="No results." />}
              onItemSelect={openRoleDialog}
              popoverProps={{ minimal: true }}
            >
              <Button text="Select a role..." rightIcon="double-caret-vertical" />
            </RoleSelect>
          </div>
        } />
        <Tab id="createRole" title="Create Role" panel={
          <div>
            <h3>Create New Role</h3>
            <FormGroup label="New Role Name">
              <InputGroup value={newRoleName} onChange={(e) => setNewRoleName(e.target.value)} />
              <Button onClick={createNewRole}>Create Role</Button>
            </FormGroup>
          </div>
        } />
      </Tabs>

      {/* User Dialog */}
      {selectedUser && (
        <Dialog
          isOpen={isUserDialogOpen}
          onClose={closeUserDialog}
          title={`Edit Roles for ${selectedUser.name}`}
        >
          <div className="bp3-dialog-body">
            <MultiSelect
              items={roles}
              itemRenderer={(role, { handleClick }) => (
                <MenuItem key={role.id} text={role.name} onClick={handleClick} />
              )}
              tagRenderer={(role) => role.name}
              selectedItems={selectedUser.roles}
              onItemSelect={(role) => handleRoleChange([...selectedUser.roles, role])}
              tagInputProps={{
                onRemove: (tag, index) => {
                  const roleToRemove = selectedUser.roles[index];
                  handleRoleChange(selectedUser.roles.filter(r => r.id !== roleToRemove.id));
                }
              }}
            />
          </div>
          <div className="bp3-dialog-footer">
            <Button onClick={closeUserDialog}>Close</Button>
          </div>
        </Dialog>
      )}

      {/* Role Dialog */}
      {selectedRole && (
        <Dialog
          isOpen={isRoleDialogOpen}
          onClose={closeRoleDialog}
          title={`Edit Permissions for ${selectedRole.name}`}
        >
          <div className="bp3-dialog-body">
            <MultiSelect
              items={permissions}
              itemRenderer={(permission, { handleClick }) => (
                <MenuItem key={permission.id} text={permission.name} onClick={handleClick} />
              )}
              tagRenderer={(permission) => permission.name}
              selectedItems={selectedRole.permissions}
              onItemSelect={(permission) => handlePermissionChange([...selectedRole.permissions, permission])}
              tagInputProps={{
                onRemove: (tag, index) => {
                  const permissionToRemove = selectedRole.permissions[index];
                  handlePermissionChange(selectedRole.permissions.filter(p => p.id !== permissionToRemove.id));
                }
              }}
            />
          </div>
          <div className="bp3-dialog-footer">
            <Button onClick={closeRoleDialog}>Close</Button>
          </div>
        </Dialog>
      )}
    </div>
  );
};

export default UserRoleAssignment;
