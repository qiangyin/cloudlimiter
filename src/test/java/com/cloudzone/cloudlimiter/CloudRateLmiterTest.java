package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.rate.CloudRateLimiter;
import com.cloudzone.cloudlimiter.rate.CloudTicker;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public class CloudRateLmiterTest {

    public static void main(String[] args) {
        final CloudRateLimiter cloudRateLimiter = CloudRateLimiter.create(10);

        // 虽然等待了3秒钟，能够获取30个令牌，但是最多只能够保留1秒的令牌，因此只能保留10个令牌
        CloudTicker.sleepSeconds(3);
        printTime(); // 0秒
        cloudRateLimiter.acquire(5); // 无阻塞消费了5个，还剩10-5个令牌
        printTime(); // 0秒

        CloudTicker.sleepSeconds(1); // 等待1秒，剩余令牌又变为了10个
        // 直接将剩余的10个令牌消费，还有25个需要等待（2.5秒）
        cloudRateLimiter.acquire(35); //
        printTime(); // 0 + 1 + 2.5 = 3.5秒
        CloudTicker.sleepSeconds(1);

        cloudRateLimiter.acquire(14); // 0
        printTime(); // 3
        CloudTicker.sleepSeconds(1);

        cloudRateLimiter.acquire(5); // 5
        printTime(); // 4
        CloudTicker.sleepSeconds(1);

        cloudRateLimiter.acquire(25); // 还需要等待25-5=20个
        printTime(); //6
        CloudTicker.sleepSeconds(1);

        cloudRateLimiter.acquire(50); // 还需要等待25-5=20个
        printTime(); //6
        CloudTicker.sleepSeconds(1);

        cloudRateLimiter.acquire(25); // 还需要等待25-5=20个
        printTime(); //6
        CloudTicker.sleepSeconds(5);

        cloudRateLimiter.acquire(1); // 还需要等待25-5=20个
        printTime(); //6
        CloudTicker.sleepSeconds(1);

        /*System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        CloudTicker.sleepSeconds(5);
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

        CloudTicker.sleepSeconds(0.5);
        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        CloudTicker.sleepSeconds(0.5);

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());
        cloudRateLimiter.acquire();

        System.out.println((new Date()).getTime());*/


    }

    static void printTime() {
        System.out.println(new SimpleDateFormat("ss:SSS").format(new Date()));
    }
}
