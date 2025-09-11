/**
 * RAG系统API调用工具
 * 提供与后端API交互的JavaScript方法
 */
class RagApi {
    /**
     * 构造函数
     * 
     * @param {string} baseUrl - API基础URL，默认为当前域名下的/api/rag
     */
    constructor(baseUrl = '/api/rag') {
        this.baseUrl = baseUrl;
        this.userId = localStorage.getItem('rag_user_id') || this.generateUserId();
        this.sessionId = localStorage.getItem('rag_session_id') || this.generateSessionId();
    }
    
    /**
     * 发送查询请求
     * 
     * @param {string} query - 用户查询文本
     * @param {Object} params - 查询参数
     * @returns {Promise} 响应承诺
     */
    async sendQuery(query, params = {}) {
        const endpoint = `${this.baseUrl}/query`;
        const requestData = {
            query: query,
            userId: this.userId,
            sessionId: this.sessionId,
            params: params
        };
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('查询请求出错:', error);
            throw error;
        }
    }
    
    /**
     * 发送异步查询请求
     * 
     * @param {string} query - 用户查询文本
     * @param {Object} params - 查询参数
     * @returns {Promise} 响应承诺
     */
    async sendQueryAsync(query, params = {}) {
        const endpoint = `${this.baseUrl}/query/async`;
        const requestData = {
            query: query,
            userId: this.userId,
            sessionId: this.sessionId,
            params: params
        };
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('异步查询请求出错:', error);
            throw error;
        }
    }
    
    /**
     * 上传文件
     * 
     * @param {File} file - 文件对象
     * @param {Object} metadata - 文件元数据
     * @returns {Promise} 响应承诺
     */
    async uploadDocument(file, metadata = {}) {
        const endpoint = `${this.baseUrl}/documents/upload`;
        const formData = new FormData();
        formData.append('file', file);
        
        if (Object.keys(metadata).length > 0) {
            formData.append('metadata', JSON.stringify(metadata));
        }
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('文件上传出错:', error);
            throw error;
        }
    }
    
    /**
     * 从URL添加文档
     * 
     * @param {string} url - 文档URL
     * @param {Object} metadata - 文档元数据
     * @returns {Promise} 响应承诺
     */
    async addDocumentFromUrl(url, metadata = {}) {
        const endpoint = `${this.baseUrl}/documents/url`;
        const requestData = {
            url: url,
            metadata: metadata
        };
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('URL文档添加出错:', error);
            throw error;
        }
    }
    
    /**
     * 添加文本文档
     * 
     * @param {string} title - 文档标题
     * @param {string} content - 文档内容
     * @param {Object} metadata - 文档元数据
     * @returns {Promise} 响应承诺
     */
    async addDocumentFromText(title, content, metadata = {}) {
        const endpoint = `${this.baseUrl}/documents/text`;
        const requestData = {
            title: title,
            content: content,
            metadata: metadata
        };
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('文本文档添加出错:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档列表
     * 
     * @param {number} page - 页码，从1开始
     * @param {number} size - 每页大小
     * @param {string} status - 可选的状态过滤
     * @returns {Promise} 响应承诺
     */
    async getDocuments(page = 1, size = 10, status = null) {
        let endpoint = `${this.baseUrl}/documents?page=${page}&size=${size}`;
        
        if (status) {
            endpoint += `&status=${encodeURIComponent(status)}`;
        }
        
        try {
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档列表出错:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档详情
     * 
     * @param {number} id - 文档ID
     * @returns {Promise} 响应承诺
     */
    async getDocument(id) {
        const endpoint = `${this.baseUrl}/documents/${id}`;
        
        try {
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档详情出错:', error);
            throw error;
        }
    }
    
    /**
     * 获取文档内容片段
     * 
     * @param {number} id - 文档ID
     * @returns {Promise} 响应承诺
     */
    async getDocumentChunks(id) {
        const endpoint = `${this.baseUrl}/documents/${id}/chunks`;
        
        try {
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取文档片段出错:', error);
            throw error;
        }
    }
    
    /**
     * 删除文档
     * 
     * @param {number} id - 文档ID
     * @returns {Promise} 响应承诺
     */
    async deleteDocument(id) {
        const endpoint = `${this.baseUrl}/documents/${id}`;
        
        try {
            const response = await fetch(endpoint, {
                method: 'DELETE'
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return true;
        } catch (error) {
            console.error('删除文档出错:', error);
            throw error;
        }
    }
    
    /**
     * 获取查询历史
     * 
     * @param {number} limit - 返回记录数限制
     * @returns {Promise} 响应承诺
     */
    async getQueryHistory(limit = 10) {
        const endpoint = `${this.baseUrl}/history/session/${this.sessionId}?limit=${limit}`;
        
        try {
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取查询历史出错:', error);
            throw error;
        }
    }
    
    /**
     * 保存用户设置
     * 
     * @param {Object} settings - 设置参数
     * @returns {Promise} 响应承诺
     */
    async saveUserSettings(settings) {
        const endpoint = `${this.baseUrl}/settings/${this.userId}`;
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(settings)
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('保存用户设置出错:', error);
            throw error;
        }
    }
    
    /**
     * 获取用户设置
     * 
     * @returns {Promise} 响应承诺
     */
    async getUserSettings() {
        const endpoint = `${this.baseUrl}/settings/${this.userId}`;
        
        try {
            const response = await fetch(endpoint);
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取用户设置出错:', error);
            throw error;
        }
    }
    
    /**
     * 生成用户ID
     * 
     * @returns {string} 生成的用户ID
     */
    generateUserId() {
        const userId = 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('rag_user_id', userId);
        return userId;
    }
    
    /**
     * 生成会话ID
     * 
     * @returns {string} 生成的会话ID
     */
    generateSessionId() {
        const sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('rag_session_id', sessionId);
        return sessionId;
    }
} 