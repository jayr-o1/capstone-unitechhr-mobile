const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Function to mark a user's email as verified
exports.verifyEmail = functions.https.onCall(async (data, context) => {
    try {
        // Security check - only allow authenticated users to verify their own email
        if (!context.auth) {
            throw new functions.https.HttpsError(
                "unauthenticated",
                "The function must be called while authenticated."
            );
        }

        const uid = data.uid;

        // Ensure the user is trying to verify their own email
        if (uid !== context.auth.uid) {
            throw new functions.https.HttpsError(
                "permission-denied",
                "Users can only verify their own email."
            );
        }

        // Verify the code from Firestore
        const verificationDoc = await admin
            .firestore()
            .collection("verification_codes")
            .doc(uid)
            .get();

        if (!verificationDoc.exists) {
            throw new functions.https.HttpsError(
                "not-found",
                "Verification record not found."
            );
        }

        const verificationData = verificationDoc.data();
        const storedCode = verificationData.code;
        const timestamp = verificationData.timestamp;
        const currentTime = Date.now();
        const providedCode = data.code;

        // Check if the code is valid and not expired (10 minutes validity)
        const TEN_MINUTES_IN_MS = 10 * 60 * 1000;
        if (storedCode !== providedCode) {
            throw new functions.https.HttpsError(
                "invalid-argument",
                "Invalid verification code."
            );
        }

        if (currentTime - timestamp > TEN_MINUTES_IN_MS) {
            throw new functions.https.HttpsError(
                "deadline-exceeded",
                "Verification code has expired. Please request a new one."
            );
        }

        // Mark the email as verified in Firebase Auth
        await admin.auth().updateUser(uid, {
            emailVerified: true,
        });

        // Update Firestore record to mark as verified
        await admin
            .firestore()
            .collection("verification_codes")
            .doc(uid)
            .update({
                verified: true,
            });

        return { success: true };
    } catch (error) {
        console.error("Error verifying email:", error);
        throw new functions.https.HttpsError(
            "internal",
            error.message || "An unknown error occurred while verifying email."
        );
    }
});

// Function to resend a verification code
exports.resendVerificationCode = functions.https.onCall(
    async (data, context) => {
        try {
            // Security check - only allow authenticated users to resend their own verification code
            if (!context.auth) {
                throw new functions.https.HttpsError(
                    "unauthenticated",
                    "The function must be called while authenticated."
                );
            }

            const uid = context.auth.uid;

            // Generate a new 6-digit verification code
            const verificationCode = Math.floor(
                100000 + Math.random() * 900000
            ).toString();

            // Update the verification code in Firestore
            await admin
                .firestore()
                .collection("verification_codes")
                .doc(uid)
                .update({
                    code: verificationCode,
                    timestamp: Date.now(),
                });

            return { code: verificationCode };
        } catch (error) {
            console.error("Error resending verification code:", error);
            throw new functions.https.HttpsError(
                "internal",
                error.message ||
                    "An unknown error occurred while resending verification code."
            );
        }
    }
);

// Function to send verification email
exports.sendVerificationEmail = functions.https.onCall(
    async (data, context) => {
        try {
            // Get email parameters from the request
            const { email, code, name } = data;

            if (!email || !code) {
                throw new functions.https.HttpsError(
                    "invalid-argument",
                    "Email and verification code are required."
                );
            }

            // Implementation with nodemailer
            const nodemailer = require("nodemailer");

            // Create a transporter with Gmail or your preferred service
            // For Gmail, you need to use an app password if 2FA is enabled
            // https://support.google.com/accounts/answer/185833
            const transporter = nodemailer.createTransport({
                service: "gmail",
                auth: {
                    user: "olores.jayrm@gmail.com", // Replace with your email
                    pass: "zpoz gyqc zzyc yiql", // Replace with your app password
                },
            });

            // Email template with nice formatting
            const mailOptions = {
                from: '"UniTech HR" <olores.jayrm@gmail.com>',
                to: email,
                subject: "Verify Your UniTech HR Account",
                html: `
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <h1 style="color: #4a6ee0;">UniTech HR</h1>
                    </div>
                    <h2 style="color: #4a6ee0;">Welcome to UniTech HR, ${name}!</h2>
                    <p>Thank you for registering. To complete your account setup, please verify your email address using the code below:</p>
                    <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0;">
                        <h1 style="color: #4a6ee0; letter-spacing: 5px; font-size: 32px;">${code}</h1>
                    </div>
                    <p>This code will expire in 10 minutes.</p>
                    <p>If you didn't create an account with UniTech HR, you can safely ignore this email.</p>
                    <div style="margin-top: 30px; text-align: center; color: #888; font-size: 12px;">
                        <p>© ${new Date().getFullYear()} UniTech HR. All rights reserved.</p>
                    </div>
                </div>
            `,
            };

            // Send the email and log the attempt
            console.log(
                `Sending verification email to ${email} with code ${code}`
            );
            await transporter.sendMail(mailOptions);
            console.log(`Email sent successfully to ${email}`);

            return { success: true };
        } catch (error) {
            console.error("Error sending verification email:", error);
            throw new functions.https.HttpsError(
                "internal",
                error.message ||
                    "An unknown error occurred while sending verification email."
            );
        }
    }
);
