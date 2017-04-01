package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.rate.CloudRateLimiter;

import java.util.Date;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public class CloudRateLmiterTest {

    public static void main(String[] args) {
        final CloudRateLimiter cloudRateLimiter = CloudRateLimiter.create(10);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire(5);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire(5);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire(14);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();



        /*System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        CloudTicker.sleepSecondsUninterruptibly(5);
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());

        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        CloudTicker.sleepSecondsUninterruptibly(0.5);
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        CloudTicker.sleepSecondsUninterruptibly(0.5);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());*/


    }
}
