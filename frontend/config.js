const config = {
    api: {
        baseUrl: 'http://localhost:8080/api',
        endpoints: {
            emailVerification: {
                send: '/email-verification/send',
                verify: '/email-verification/verify'
            },
            subscriptions: {
                create: '/subscriptions',
                manage: '/subscriptions/manage',
                delete: (id) => `/subscriptions/manage/${id}`
            }
        }
    },
    service: {
        name: 'FeedPing',
        description: 'RSS 피드를 통한 스마트한 정보 구독 서비스',
        copyright: '© 2024 FeedPing. All rights reserved.'
    },
    validation: {
        email: {
            pattern: '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$',
            maxLength: 255
        },
        verificationCode: {
            length: 6
        },
        url: {
            maxLength: 4096
        },
        siteName: {
            maxLength: 255
        }
    }
};

window.APP_CONFIG = config;
