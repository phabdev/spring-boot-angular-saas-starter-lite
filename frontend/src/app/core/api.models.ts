export type Role = 'USER' | 'ADMIN';
export interface User { id: string; email: string; displayName: string; role: Role; }
export interface Session { accessToken: string; tokenType: 'Bearer'; expiresIn: number; user: User; }
export interface Project { id: string; name: string; description: string; createdAt: string; updatedAt: string; }
export interface ProjectInput { name: string; description: string; }
