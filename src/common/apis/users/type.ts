export interface UserInfo {
  id: number
  username: string
  realName: string
  role: number
  roleName: string
  orgName: string
  roles: string[]
  permissions: string[]
}

export type CurrentUserResponseData = ApiResponseData<UserInfo>
