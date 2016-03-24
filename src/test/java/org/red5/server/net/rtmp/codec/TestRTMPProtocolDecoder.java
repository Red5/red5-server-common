package org.red5.server.net.rtmp.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.red5.server.net.rtmp.IRTMPHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRTMPProtocolDecoder implements IRTMPHandler {

    protected Logger log = LoggerFactory.getLogger(TestRTMPProtocolDecoder.class);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

//    @Test
//    public void testDecodeBuffer0() {
//        log.debug("\ntestDecodeBuffer0");
//        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
//        RTMPConnection conn = new RTMPMinaConnection();
//        conn.getState().setState(RTMP.STATE_CONNECTED);
//        conn.setHandler(this);
//        IoBuffer p0 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c79f74365db065581bb60c7ea6eb849812a4cea7e0757375c2ed5c6c316c4238aa6803d7dfe52284f00a336d1e01fb92e2e71e83149e104f64a51878b2192606f0efd8a0bf94bd379f62215fdb300fbaea9d2bfce21ebc169f65d6833872af32767f85629be8e8fafcf41e0722905559477ff638fe6d26da6a9c25a18253378b70b9504e9ae25c678bd8b44f4bcb6afc7c056dea914b9b760a08b7995bd376549f917af074e060fe24dd66bd3e9e3b39c3ac255cf1813c0f08c735077152b5eaee97f0ad22ba544ddd2beb36079afb1048860a3123705372446ff456e4a2ab75f05849ad91a519443c72f3c8c5b60266cf16110d3b69b41c8f7f6584c3f5a049756dfd0b0024dc494b049a35883fbb305e00cb8bb79d09462cc70266bddb31e798083ff2ee6a4731fd903a55033b12a482144d2be9797dd14d67b691ec543cda0ad8a10cf03bcee9e67e999ee109726f558fb8e29d56030adaf38d34c5f406ca3c0a050e40ec2ed5ca8d6728b6c78af9f9cc1dea46b98b2edba431116cca00b127d47bb22793356af9eedd2cb2580cb0d6e7bfb8d86297703be9a679bb260e5db5074cf1930175bf605acd2dee7b08957ca0967a6646519d3beae59ae9004df22a932d38fce93f38532e1406239d5bdbd19a63b0041e66da74b90ec231ed7c306bef2eb198acdc34012507a54c57fc61b9da417f4493f432762230e0da13de8d182adba1ad2d8960395ff9e93351db5f4998843c1045c6e83c35f19e406d056f838c48345c08635a537bd09364121953cea315bcbff2183b30404a16e29bce2cbc0711e2d47c87036e4fd89ada65edd7baded0135ff9f2ad6202c015f2e4ca6d818c4dc22072362bc78f0807968c0d8b91651158e5585d325ce1906ac330cc7871a155dec33e35cbd244da2ace33922eec32885656506d56be20e90b5737102051d77d5228b4f39fe041d00cbd7522c2b0680e29c64fabae611ed4ae28d1cecfe65ac68b2d7ac0907879e8d7558336441a251e055d33754abd2f21f4b34efc94ad97a38d4773398aa827488318537bd8d823d34fce6f02a203753dcd96fa67ecfd4af2a56f9b7270d19e04aa270298985c2c4035454d98f758dd8cd7372dc973481943e3b128047c323c09a72f91605efa9546d8eb649f035985632b3d76058113189886755c283d7629cd5c3f5dc4312e1a795b4f834764926fc22dc18012d7c2d30e1e2f60be9d42be4baf22a1839f815a701432448cbc3590902350006517c4692edd50e4f375498684f269cf85034e2d15ea39eb29d92694e80f773a41e48037ae7eb472d98f46fc981a9fc362ec8718603ec65ab656648bfa77b0eb88e8c7a14204614b605d2e35fb921523b3332e6b4e3353ef2ce843382962bab274ca6bf215f7b639faa9e84e815c2ea2afa95cce5dfaadd3a5b760c73a43659a5312cdfdb6a0f5314e86d6c3212a5c20357cfe5a42f6708f0d55b087c6b0324e385efcef96987b89d39d4773e44e632380208f6c8e9e74a04b2a98dd4981126e42c9e9af1ab4c3f838942dee867d5765f880bd450d665e0576d1f0fb9272d5f0184070f0732a8180e2467baab43ac5873fe141b4986463debaacebb15e1a3c73173c29e192de3c9cfd6511f49c771c37b8fa728812ed82b9fb88058569646273a81b1c302984e4dad17a07726a07f8485170c81f6946da7c7b6ed8d840d5461dd7f1655320f277ed083419ef937c3786db817aa7099be06d900de4a666e0b7f0fd40545e4746bc9e8b0496ef29832cd82b7da3b1c4cbcadfccf10a7d567b468dc25b20befcd7bf59ee43b94bf867bbbee1272bbc2c75470d1d45afddd81ace04117da0a230e1c4c4aabdffe5cc17ff3086c73b52a1a68ad47219a270a98dc9cba430ca2bca09197cf23314c24872e65c8552836fe9472d087d015161fd25b2401564a63d476d36776b170d39b1c2f65f77358228933111d0158a680fd9073e06813b3880121dd8938c0112cd491422a72bf0eae1f0717cb1b10d52ac2d034efd22ab305fa090136647cf5b6c7c55d16d9b29a7d48b701a76586305cd95bd402e96944c4094eb2963fc8b586f873057979df590262a725d6881c167bce59c944bbe9868dfb8aebea1850e2c3e7bada7a8f5d5339f1368bbb92ca9196f4f4026330f2e030000010001321400000000020007636f6e6e656374003ff00000000000000300036170700200086f666c6144656d6f0008666c61736856657202000e4c4e582032312c302c302c313832000673776655726c020029687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e7377660005746355726c02001972746dc3703a2f2f6c6f63616c686f73742f6f666c6144656d6f0004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766964656f46756e6374696f6e003ff000000000000000077061676555c3726c02002a687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e68746d6c000009000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"));
//        List<Object> objs = dec.decodeBuffer(conn, p0);
//        log.debug("Objects #0: {}", objs);
//    }

    @Test
    public void testDecodeBuffer() {
        log.debug("\ntestDecodeBuffer");
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        List<Object> objs;
        RTMPConnection conn = new RTMPMinaConnection();
        conn.getState().setState(RTMP.STATE_CONNECTED);
        conn.setHandler(this);
        IoBuffer p00 = IoBuffer
                .wrap(IOUtils
                        .hexStringToByteArray("8639ae8685ad4e802fb905a7918b480416b013e0632e41773e6ba30a1ee089ddb94df6eff6b59aa6251a2707215a2907dc6b51a749ff6680b37792b6c302d43e80f4780361ab7b3c79de5f917aece673e6192e0d45606a5a56dc23ee8113b3381a5d428074f461db71f8caaefa650517809b63edc8412c1f88b4e4d43584ca491a89dfdbb78df1b00c1cedc8e2139fe831becd70527adf4d17760c1cbfff47a1116aaa8f603d3f5319a0688bbc215c1cae1cfb06b6546abd4b76cc7f32cbc84b4531ba2f539d6ae5c4c081bcc51a73b7c14881f8b6bddb480d8a4430b98577f15d31215480d6ebb9cb56931ad3703c2bf024c943b3c45c18717d467387141cdcc88f389548bc335b10ead0daf8e8c69e67f43099ab1f5f2afcb343c08d4b065ec7ce3a437d9891ec8388155d5bdae8dcfd6b4419df2d9c7ca584511e240cc6b1f04ccc098a74d5037c921e4f5916affa17ff71daed20d621687b12bd3f7fcb8f538360b39eb237ca05ada13dc433dc10c2504c55e5ca2e6528b3279eb49e4629218acfc33fce315c58533076d7e3fff6e49650d4283c8fa4ee766f93fc3d10b10bd10b8c2393e2a1da4e6c53dcc5ddbd6efd7b844b7546b0472346b9229cdb6e8d45f70f1e6d23b96cbb2c41fcd486e25fef021e19568c4617248e58ca195fb7aad5ca34dcf27ac7f03dc71a8b4831d1bbb273f6bba56ed9cdf49eef6c45df5071d19d9e8e23fb33b34a65648141cd61db72f79fa7944e3232cd8e30ba310d566fdac6d51b683a3c27af315ca5dc0f614d62c6672e97c036d181e00d6cf08b41a130d7a24783ad616593dc10753959529657f94aec63fa073ae24afe44b2d7075164ebc25305db954607b92c1c7ded5f88e587227ce411010292b6e392aeaeca456d64dadec73cac5de51ec2d3b530db0cdb9869fbdabe1f0aae10bcc1479dc927178a323ac0b2c1def11a086605ee0a66d5a9f1652b5b5860cdbd3594c8e2511c004d8704ccb2732197f37e687753ce0ed00717f20ca6e7076fd4d06b49ba8c665a2adc41e702753225868ace398baebb6944c29c149b838fb71b1e8c20ed3c9561129867c56bc800be10f8dba659f37bbe2242b0cbdcf342396e5ee61f327e7759500d4647e313937c81809b602af08493812ca199698ed35de9ebeded83e1ef5d8f8ad56c368acd7a6d7a05118541bf9d60bc3e45d64431a7700770c814f48f5dcd1c35270a5288b8cbde9333b5b8c40dfd6f0d31d51bea52e0bc6e6e4f46b1bfe85268a494a83d20668829dcfa492325e5f372f45a74bc19c9be07eda5bb1060376b0563081f541801b7d7ce5f7408e5bb11f63d0638419f7cb8aaaa55afa32dfb1e4f0df11fc355ad6c30405b8824f8fff87feeb3b5390cde76040008bb422200a571cd0c19fda9df620ad6ca881c833728081e7cefecc78b280cc06b4c7de3d1c9cebc04d84e1c43fb9fc8b52cc07e0eb15012872c0739322b1da9d7888b6c0f968863ac571837fd63457f98d574665e3cbe1f3b497fb03d1ab8e481985732de26051919d0aba7f18281d8f512b05a625746e4dd50e0c4be16ab4b59aa3cf24e1648ee9d74e4f3b2a92fc7828a5eb2040a706d52e354fb079df3062d404c46fb78de4931caac381020c5bf6b38fecdc5f7d66c3ea3fb087f4ed8ac98a81a24a42053282c7524c2953a5885c1ff5d736d46c472d318312f2a427dc3526fd513c88dbb8583d607031c0a2825f9e74428b859d8874e0cb013b458b27d549a41e4330682dac90e073b79c458dcb2cba317f816bd2dabdcee96d77ab6466021a23a932d134ba33279956aae938787a3a59216b53d50dc0554fe34b5f51d1e050f84e03a7442b29604684291a2ee79198a3911f8a54a1e47c0bf9ec25114f60dae43d4aa4639e5a5c89010ba3884014c2721cab878795f22ba8136f7d4685c9a5c348bda08b60a1ff1afe6d5578d52f5051eeafa9e3b9501701a272a880aaeb30fbc2db66a5e48d7c811a56c9a809c92567d8a10472c142f3dc38c0123e20ed3feb067a550e0a997b38061e191a3bb5b47a04ef70fdd94e69bfc9be160d8a8cc7dc163e8d595cc987c1d676a7b543f56305be60921c19113be5ea988c864b636e216c1c6d71319e0c96b58eb619ac63016ffb97761b79a3eea0016cfacadc7c10300000100014d1400000000020007636f6e6e656374003ff0000000000000030003617070020003766f640008666c61736856657202000e4c4e582032302c302c302c323836000673776655726c020036687474703a2f2f6c6f63616c686f73743a353038302f766f642f6d696e69706c617965722e7377662f5b5b44594e414d49435d5d2f320005746355c3726c02001972746d703a2f2f6c6f63616c686f73743a313933352f766f640004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766964656f46756e6374696f6e003ff00000000000c30000077061676555726c020024687474703a2f2f6c6f63616c686f73743a353038302f766f642f696e6465782e68746d6c000e6f626a656374456e636f64696e67004008000000000000000009"));
        p00.position(1536);
        objs = dec.decodeBuffer(conn, p00);
        log.debug("Objects #00: {}", objs);
        assertNotNull("Objects should not be null", objs);
        assertFalse("Objects should not be empty", objs.isEmpty());
        assertEquals("Method should be 'connect'", "connect", ((Invoke) ((Packet) objs.get(0)).getMessage()).getCall().getServiceMethodName());

        IoBuffer p01 = IoBuffer
                .wrap(IOUtils
                        .hexStringToByteArray("030000000001431400000000020007636f6e6e656374003ff0000000000000030003617070020003766f640008666c61736856657202000e4c4e582032302c302c302c323836000673776655726c020036687474703a2f2f6c6f63616c686f73743a353038302f766f642f6d696e69706c617965722e7377662f5b5b44594e414d49435d5d2f320005746355c3726c02001972746d703a2f2f6c6f63616c686f73743a313933352f766f640004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766964656f46756e6374696f6e003ff00000000000c30000077061676555726c02001a687474703a2f2f6c6f63616c686f73743a353038302f766f642f000e6f626a656374456e636f64696e6700400800000000000000000902fffe410000040500000000009896800300003100001a11000000000002000c63726561746553747265616d00400000000000000005"));
        objs = dec.decodeBuffer(conn, p01);
        log.debug("Objects #01: {}", objs);
    }

    @Test
    public void testDecodeBufferCreateDelete() {
        log.debug("\ntestDecodeBufferCreateDelete");
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        List<Object> objs;
        RTMPConnection conn = new RTMPMinaConnection();
        conn.getState().setState(RTMP.STATE_CONNECTED);
        conn.setHandler(this);
        for (int i = 0; i < 13; ++i) {
            conn.getState().setLastReadHeader(i, new Header()); //TODO hardcoded, so test will not fail
        }
        int idx = 0;
        for (IoBuffer p : new IoBuffer[] {
                // packet #0 // connect // 320
                IoBuffer.wrap(IOUtils.hexStringToByteArray("030000000001321400000000020007636f6e6e656374003ff00000000000000300036170700200086f666c614" +
                        "4656d6f0008666c61736856657202000e4c4e582032302c302c302c333036000673776655726c020029687474703a2f2f6c6f63616c686f73743a35303" +
                        "8302f64656d6f732f6f666c615f64656d6f2e7377660005746355726c02001972746dc3703a2f2f6c6f63616c686f73742f6f666c6144656d6f0004667" +
                        "061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f64656" +
                        "37300406f800000000000000d766964656f46756e6374696f6e003ff000000000000000077061676555c3726c02002a687474703a2f2f6c6f63616c686" +
                        "f73743a353038302f64656d6f732f6f666c615f64656d6f2e68746d6c000009"))
                // packet #1 // 16
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("02db5565000004050000000000989680"))
                // packet #2 // 59
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("0300017c00002f140000000002002264656d6f536572766963652e6765744c6973744f66417661696c61626" +
                        "c65464c567300400000000000000005"))
                // packet #3  // 14
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("42000000000006040007ce4c5f73"))
                // packet #4 // 7
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007ce4c6743"))
                // packet #5 // 7
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007ce4c6f13"))
                // packet #6 // 33
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("43001e610000191402000c63726561746553747265616d00400800000000000005"))
                // packet #7 // 18
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("4200000000000a0400030000000000001388"))
                // packet #8 // 52
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("08001fdd00001d1401000000020004706c61790000000000000000000502000973706565782e666c76c200030000000100001388"))
                // packet #9 // huge connect from OpenMeetings
                ,
                IoBuffer.wrap(IOUtils.hexStringToByteArray("030000000001b71400000000020007636f6e6e656374003ff000000000000003000361707002000e6f70656" +
                        "e6d656574696e67732f350008666c61736856657202000e4c4e582032302c302c302c323836000673776655726c020082687474703a2f2f6c6f63616c6" +
                        "86f73743a353038302f6f70656e6d656574696e67732f7075626c69632f6d61696e6465c36275672e73776631312e7377663f7769636b65747369643d3" +
                        "563333562613330396634393030346139303366323264623663323564393330266c616e67756167653d31267769636b6574726f6f6d69643d350005746" +
                        "355726c02002472746d703a2f2f6c6f63616c686f73743a313933352f6f70656e6d656574696e67732f35c30004667061640100000c6361706162696c6" +
                        "97469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766" +
                        "964656f46756e6374696f6e003ff000000000000000077061676555726c02002a687474703a2f2f6c6f63616c686f73743ac3353038302f6f70656e6d6" +
                        "56574696e67732f23726f6f6d2f35000e6f626a656374456e636f64696e670040080000000000000000090100")),
                IoBuffer.wrap(IOUtils.hexStringToByteArray("4300120c0000191402000c63726561746553747265616d00400800000000000005")),
                IoBuffer.wrap(IOUtils.hexStringToByteArray("4200000000000a0400030000000000001388830014d302000c63726561746553747265616d0040100000000" +
                        "0000005430000000000221402000c64656c65746553747265616d00000000000000000005003ff0000000000000")),
                IoBuffer.wrap(IOUtils.hexStringToByteArray("04007dc400010108010000006a42a5b3b597d08decc518618c31c688880821841022226eaaaaaaaaaafeff" +
                        "ffffffffffffffffffe17384c5790bb1ae48f4812ff082e31ce7381ccecffdfdff7f16a61525c7586c145624279c6d15b8ca145c5b11a396dc1ea405aa" +
                        "eb584a82022a4aab5c0d3ccf732381d8568f05399f98b47eb22b1a714aa116482deab090420000000000040100000101c4e3d6aed29dc420b24cd22897" +
                        "948f0d68c12e16b640bc736918d278b5956c2d44e358010e8ff699b91bd73716ecd70b891b3791263b9d380554dccec61646ad51dd5e9ed429a3a4a2b0" +
                        "51066394111179fbe53f6562f3f461cd49fb1b6b662e79535391f32d29668e3494a211ff442e2de649475ba8e480f6de4ef5b73dff6d3356")),
                IoBuffer.wrap(IOUtils.hexStringToByteArray("8400002e6a5f2bd5e6b81389f5bcd79c93cf8dc82fcde9abd1af033d756343a53488344bd53c15f9202ac59" +
                        "299268794caa179534a2132b68a1da926f5da1ad3566d43223d99437a65ba4f829395c95c94f11838650cb6169785929758aded8b6da3ac86ba422805e" +
                        "cf6ebfc0ecfa8d37a2fde5f7feaa919b154c9136251a8a4aa09753616608175d6aa9b6995a1b89a8472118833d8a6eab81572fdde05ed5f21364cd97a6" +
                        "a7abc24c6fcef545e2d2553b3152384afd567957b749125404821853a388400a2597955cb538137a5b79eab101599c9e71ad7c5d6db1039a1274771d2c" +
                        "eb2cd95eb8ac9befed41d2a79c34b630afc146979b549eaf9d50d952895f63b56ba504600008e0005010932000084074202becac8c293f8c2c3b658622" +
                        "5827a0342932209024ad6308162a489b0c0336fe9de14fe139c3630dcd66b622a6e0892b6c9673ce3a23ce9f3e7db683bcd42b304e2bc951c296bad8b8" +
                        "2f8d0e8abc3d8d271005414b939d278939dd98396c2906fffc41e6cede4d1138362cc1708f4247c30771f80acc29423a3214f3e478350dd11a6353fd95" +
                        "ae16f4157ed26c0f412c29e347a7e9f6ba6a6a994b262cd7535d3cf13e7f4f4cea6ce4e276c1878257c4ae1567630569ee959f17be28cf26064404905e" +
                        "a16a9db4a4c1633da1cb4d1fa03443cf1d3a9c1b8810a784e74ed3d189363379ec459a1cd27059851e8065a48359a23fd911974c8902087c674e893e90" +
                        "b24977641c286330b314b360d50b3a7c559d70af3a18359d17b413b79d17c7857f80ad25141621a9d3ac946706625e367789865cd02dd9abda5ad36c0b" +
                        "02bed04e90e56d4d5c456f72f43f49c870f05f8094f17f4e8859e40889d7c056030ffc9de4024ca472461a14af4621d3408e702b6b3307214f6215d102" +
                        "b35b38784f982b063a06bb5334c201c8ac5e0ddbf89fc6868d831e02f59a8b23de824852e4b9185d74d0838c9cdde42ce360c740e191a853d9180373fe" +
                        "a5905ca943bd662110c561b31c0c0e0ab380da7fc2a6c262d18a104def5a489af528a9e14f60380c3d9fe48a00adbc48a25db7122e4689071098101861" +
                        "0a318649d6bbb4089c64214068e853d1da031095162e115e167286848c5229a0529675108471b5de14f2b71c3ac92fa0eee340c740b29ab460982b74f0" +
                        "d81907fc3209224c42c3220712ad9594ed3a08b5bd1b96f5a17085be34229640bb4a5c046b69aeaec8124a3510b0e148e61e01c0b890a732a6ade87c78" +
                        "4fa30f1721063ffc5d8573a9f1b1c8984d84b296365952241988f045702b8d226c43582a6f943867bd5d869a7254fc8902e0a7f74f66a509f172ae9684" +
                        "a0c83f9b85291b88915150ac29e1c62288cd0e34725a889f288488d0369ff91a0fec4fbc5ae74682e0a17cc4408c186874d82446f596b66b170118e233" +
                        "614f1069ec2160d9625312cdda23a602790a22152f301c64fa45ccc60c27c04d1ba240543511d2b6df077dc85a88640dd80850fa2c0a18dcd6c0cacd4d" +
                        "88fcd3da04c15fc19b61a0238d443c98a04314c0dc06b2878dacd2c23cea36c02042c15c35480c940f4eb189c3cc4dc1a27ac254fd69a2db187853f899" +
                        "3afca21a7321e09c15a59a95938de276b4840b21e831d02c994e81195f48c29edf4ca764190ff328aae09c21f6825318ce242b061608ff160636046414" +
                        "bb02fb8ce836effd2b1c60ce6d5ef53b1c56d830c05c27c9d606414f0b0ac119e1787220d25b3195dbd68a38d778b1e1d6b5ab022854142db0685f9822" +
                        "6b68da2663538884a300a7a146b1c6162af268051846e8c2b06280832d6be1c8653476048c618582fde872329ee8d029f352ea4e44a1422292a6342ff2" +
                        "b641840306960cffd32c3da0436394c9ad0d8300a796a6ab0b574c6160ddda03c49ff015490d9d0a7f3bbc01fcc304d46e31ffba0c27ea86e8191beac9" +
                        "8dc16053ce26b418e7f150a744602650ea227ab7b15b6052d1760bc29e8da4637c4b0f60dd191e8343050186fe0699fae01dc480d2c27563014f27659e" +
                        "b5630154ad06e19286dff5fa22034c052f18d3214f593616c467b06c2d95a44fc11c088bc14f962903b9d6fb0a1819e1442bc220a7b1437780d061c085" +
                        "b01488dde8dd4ea0b55fbd89076673ba16853fa38601907f7a20f4f45c1a3fe06b7f96a9623efbdd44f06360948c4")) }) {
            objs = dec.decodeBuffer(conn, p);
            log.debug("Objects #02: {}", objs);
            assertFalse("Objects should not be empty", objs.isEmpty());
            assertEquals("Buffer should be empty [idx = " + idx++ + "]", p.capacity(), p.remaining());
        }
    }

    @Test
    public void testDecodeBufferChunks() {
        log.debug("\n testDecodeBufferChunks");
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        List<Object> objs;
        RTMPConnection conn = new RTMPMinaConnection();
        conn.getState().setState(RTMP.STATE_CONNECTED);
        conn.setHandler(this);
        IoBuffer p00 = IoBuffer.wrap(IOUtils.hexStringToByteArray("030000000001531400000000020007636f6e6e656374003ff0000000000000030003617070020000000e6f626a656374456e636f64696e6700000000000000000000046670616401000008666c61736856657202001057494e2031312c322c3230322c3233350005746355726c02001b72746d703a2f2f36372e3136372e3136382e3138323a313933352f00c30b617564696f436f646563730040abee000000000000077061676555726c05000b636c7573746572506173730200086368616e67656d65000f70726976617465496e7374616e6365010000087075626c6963497002000d35342e3230392e32342e323138000a7075626c6963506f727400409e3c0000000000000d766964656fc346756e6374696f6e003ff0000000000000000470617468020000000c6361706162696c697469657300402e000000000000000673776655726c05000b766964656f436f64656373000000000000000000000009"));
        objs = dec.decodeBuffer(conn, p00);
        log.debug("Objects #00: {}", objs);
        assertNotNull("Objects should not be null", objs);
        assertFalse("Objects should not be empty", objs.isEmpty());
        assertEquals("Method should be 'connect'", "connect", ((Invoke) ((Packet) objs.get(0)).getMessage()).getCall().getServiceMethodName());
    }

/*
    @Test
    public void decodeBigPacket() throws Exception {
        log.debug("\n decodeBigPacket");
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        RTMPConnection conn = new RTMPMinaConnection();
        conn.getState().setState(RTMP.STATE_CONNECTED);
        conn.setHandler(this);
        Channel six = conn.getChannel(6);
        log.trace("Channel six? {}", six);
        RTMPDecodeState state = conn.getDecoderState();
        
        IoBuffer in = IoBuffer.allocate(0);
        in.setAutoExpand(true);
        fillBufferFromStringData(in, "bigpacket.dat");
        int loops = 0;
        int packetCount = 0;
        do {
            log.debug("Start buffer - pos: {} limit: {} remaining: {}", in.position(), in.limit(), in.remaining());
            Packet pkt = dec.decodePacket(conn, state, in);
            if (pkt != null) {
                log.debug("Decoded: {}", pkt);
                packetCount++;
            }
            log.debug("End buffer - pos: {} limit: {} remaining: {}", in.position(), in.limit(), in.remaining());
        } while (in.hasRemaining() && loops++ < 25);
        log.info("Decoded packet count: {}", packetCount);
    }
    
    @Test
    public void decodeBigPacketInPieces() throws Exception {
        log.debug("\n decodeBigPacketInPieces");
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        RTMPConnection conn = new RTMPMinaConnection();
        conn.getState().setState(RTMP.STATE_CONNECTED);
        conn.setHandler(this);
        Channel six = conn.getChannel(6);
        log.trace("Channel six? {}", six);
        RTMPDecodeState state = conn.getDecoderState();
        // tmp storage
        IoBuffer tmp = IoBuffer.allocate(0);
        tmp.setAutoExpand(true);
        fillBufferFromStringData(tmp, "bigpacket.dat");
        tmp.mark();
        // actual input
        IoBuffer b0 = IoBuffer.allocate(2);
        tmp.setAutoExpand(true);
        b0.put(tmp.get());
        b0.flip();
        Packet pkt = dec.decodePacket(conn, state, b0);
        assertTrue(pkt == null);
        tmp.reset();
        // add the 2 bytes
        IoBuffer b1 = IoBuffer.allocate(5);
        b1.put(tmp.get());
        b1.put(tmp.get());
        b1.put(tmp.get());
        b1.put(tmp.get());
        b1.put(tmp.get());
        b1.flip();
        pkt = dec.decodePacket(conn, state, b1);
        assertTrue(pkt == null);
        
    }
*/

    @Override
    public void connectionOpened(RTMPConnection conn) {
        log.debug("connectionOpened - conn: {}", conn);
    }

    @Override
    public void messageReceived(RTMPConnection conn, Packet packet) throws Exception {
        log.debug("messageReceived - conn: {} packet: {}", conn, packet);
    }

    @Override
    public void messageSent(RTMPConnection conn, Packet packet) {
        log.debug("messageSent - conn: {} packet: {}", conn, packet);
    }

    @Override
    public void connectionClosed(RTMPConnection conn) {
        log.debug("connectionClosed - conn: {}", conn);
    }

    @SuppressWarnings("unused")
    private void fillBufferFromStringData(IoBuffer buf, String byteDumpFile) throws Exception {
        File f = new File(String.format("%s/target/test-classes/%s", System.getProperty("user.dir"), byteDumpFile));
        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                buf.put(IOUtils.hexStringToByteArray(line));
            }
            buf.flip();
            log.debug("Filled buffer: {}", buf);
        } finally {
            in.close();
        }
    }

}
