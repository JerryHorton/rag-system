/**
 * 企业级RAG系统前端API客户端
 * 用于与后端API交互
 * 
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
class RagClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl || window.location.origin;
        this.apiBase = `${this.baseUrl}/api/rag`;
        
        // 从localStorage获取会话ID
        this.sessionId = this.getOrCreateSessionId();
        this.userId = localStorage.getItem('rag_user_id') || 'anonymous';
    }
    
    /**
     * 获取或创建会话ID
     */
    getOrCreateSessionId() {
        let sessionId = localStorage.getItem('rag_session_id');
        if (!sessionId) {
            sessionId = 'session_' + new Date().getTime();
            localStorage.setItem('rag_session_id', sessionId);
        }
        return sessionId;
    }
    
    /**
     * 设置用户ID
     */
    setUserId(userId) {
        this.userId = userId;
        localStorage.setItem('rag_user_id', userId);
    }
    
    /**
     * 发送查询请求
     * 
     * @param {string} query 查询文本
     * @param {object} params 查询参数
     * @returns {Promise} 返回Promise
     */
    async sendQuery(query, params = {}) {
        try {
            const response = await fetch(`${this.apiBase}/query`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    query: query,
                    userId: this.userId,
                    sessionId: this.sessionId,
                    params: params
                })
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('查询请求失败:', error);
            throw error;
        }
    }
    
    /**
     * 发送异步查询请求
     * 
     * @param {string} query 查询文本
     * @param {object} params 查询参数
     * @returns {Promise} 返回Promise
     */
    async sendQueryAsync(query, params = {}) {
        try {
            const response = await fetch(`${this.apiBase}/query/async`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    query: query,
                    userId: this.userId,
                    sessionId: this.sessionId,
                    params: params
                })
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('异步查询请求失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取异步任务状态
     * 
     * @param {string} taskId 任务ID
     * @returns {Promise} 返回Promise
     */
    async getQueryTaskStatus(taskId) {
        try {
            const response = await fetch(`${this.apiBase}/query/status/${taskId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取任务状态失败:', error);
            throw error;
        }
    }
    
    /**
     * 上传文件
     * 
     * @param {File} file 文件对象
     * @param {object} metadata 元数据
     * @returns {Promise} 返回Promise
     */
    async uploadDocument(file, metadata = {}) {
        try {
            const formData = new FormData();
            formData.append('file', file);
            if (metadata) {
                formData.append('metadata', JSON.stringify(metadata));
            }
            
            const response = await fetch(`${this.apiBase}/documents/upload`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('文件上传失败:', error);
            throw error;
        }
    }
    
    /**
     * 从URL添加文档
     * 
     * @param {string} url URL地址
     * @param {object} metadata 元数据
     * @returns {Promise} 返回Promise
     */
    async addDocumentFromUrl(url, metadata = {}) {
        try {
            const response = await fetch(`${this.apiBase}/documents/url`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    url: url,
                    metadata: metadata
                })
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('URL文档添加失败:', error);
            throw error;
        }
    }
    
    /**
     * 添加文本文档
     * 
     * @param {string} title 标题
     * @param {string} content 内容
     * @param {object} metadata 元数据
     * @returns {Promise} 返回Promise
     */
    async addDocumentFromText(title, content, metadata = {}) {
        try {
            const response = await fetch(`${this.apiBase}/documents/text`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    title: title,
                    content: content,
                    metadata: metadata
                })
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('文本文档添加失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档列表
     * 
     * @param {number} page 页码
     * @param {number} size 每页大小
     * @param {string} status 可选的状态过滤
     * @returns {Promise} 返回Promise
     */
    async getDocuments(page = 1, size = 10, status = null) {
        try {
            let url = `${this.apiBase}/documents?page=${page}&size=${size}`;
            if (status) {
                url += `&status=${status}`;
            }
            
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档列表失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档详情
     * 
     * @param {number} id 文档ID
     * @returns {Promise} 返回Promise
     */
    async getDocumentById(id) {
        try {
            const response = await fetch(`${this.apiBase}/documents/${id}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档详情失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档源内容片段
     * 
     * @param {number} id 文档ID
     * @returns {Promise} 返回Promise
     */
    async getDocumentSourceChunks(id) {
        try {
            const response = await fetch(`${this.apiBase}/documents/${id}/chunks`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档源内容片段失败:', error);
            throw error;
        }
    }
    
    /**
     * 删除文档
     * 
     * @param {number} id 文档ID
     * @returns {Promise} 返回Promise
     */
    async deleteDocument(id) {
        try {
            const response = await fetch(`${this.apiBase}/documents/${id}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('删除文档失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取用户查询历史
     * 
     * @param {number} limit 返回记录数限制
     * @returns {Promise} 返回Promise
     */
    async getUserQueryHistory(limit = 10) {
        try {
            const response = await fetch(`${this.apiBase}/history/user/${this.userId}?limit=${limit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取用户查询历史失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取会话查询历史
     * 
     * @param {number} limit 返回记录数限制
     * @returns {Promise} 返回Promise
     */
    async getSessionQueryHistory(limit = 10) {
        try {
            const response = await fetch(`${this.apiBase}/history/session/${this.sessionId}?limit=${limit}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取会话查询历史失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取用户设置
     * 
     * @returns {Promise} 返回Promise
     */
    async getUserSettings() {
        try {
            const response = await fetch(`${this.apiBase}/settings/${this.userId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取用户设置失败:', error);
            throw error;
        }
    }
    
    /**
     * 保存用户设置
     * 
     * @param {object} settings 设置对象
     * @returns {Promise} 返回Promise
     */
    async saveUserSettings(settings) {
        try {
            const response = await fetch(`${this.apiBase}/settings/${this.userId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(settings)
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('保存用户设置失败:', error);
            throw error;
        }
    }
    
    /**
     * 获取系统状态
     * 
     * @returns {Promise} 返回Promise
     */
    async getSystemStatus() {
        try {
            const response = await fetch(`${this.apiBase}/system/status`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`API错误: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取系统状态失败:', error);
            throw error;
        }
    }
} 