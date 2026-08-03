export interface UserInfo {
  id: string
  username: string
  realName: string
  role: number
  roleName: string
  orgName: string
  phone?: string
  avatar?: string
  departmentId?: string | null
  departmentName?: string
  roles: string[]
  permissions: string[]
}

export type CurrentUserResponseData = ApiResponseData<UserInfo>
