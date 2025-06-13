import Vue from 'vue';
import VueRouter from 'vue-router';

// 路由懒加载

import Home from "@/views/Home.vue";
import Login from "@/views/Login.vue";
import Index from "@/views/Index";
import User from "@/views/User";
import Node from "@/views/node";
import Tunnel from "@/views/Tunnel";
import Forward from "@/views/Forward";
import Limit from "@/views/Limit";

Vue.use(VueRouter);

// 路由配置

const routes = [
    {path: '/', redirect: '/login'},
    {path: '/login', component: Login},
    {
        path: '/home', component: Home, redirect: '/index', children: [
            {path: '/index', component: Index},
            {path: '/user', component: User},
            {path: '/node', component: Node},
            {path: '/tunnel', component: Tunnel},
            {path: '/forward', component: Forward},
            {path: '/limit', component: Limit}
        ]
    },
]

const router = new VueRouter({
    base: process.env.BASE_URL,
    routes
})

router.beforeEach((to, from, next) => {
    if (to.path === '/login') return next();
    const tokenStr = localStorage.getItem('token')
    if (!tokenStr) {
        return next('/login')
    }
    next()
})


export default router
